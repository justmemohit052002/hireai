package com.vionsys.hireai.ai.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageRequest;
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.chatbot.AiChatTurn;
import com.vionsys.hireai.ai.dto.chatbot.AiExtractedFields;
import com.vionsys.hireai.ai.entity.ChatConversation;
import com.vionsys.hireai.ai.entity.ChatMessage;
import com.vionsys.hireai.ai.repository.ChatConversationRepository;
import com.vionsys.hireai.ai.repository.ChatMessageRepository;
import com.vionsys.hireai.ai.service.AiChatbotService;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.exception.CandidateNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiChatbotServiceImpl implements AiChatbotService {

    private final CandidateRepository candidateRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AiEngineClient aiEngineClient;

    @Override
    public AiChatMessageResponse sendMessage(UUID candidateUserId, String newMessage) {
        Candidate candidate = candidateRepository.findByUserId(candidateUserId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate profile not found for authenticated user."));

        ChatConversation conversation = conversationRepository.findLatestByCandidateUserId(candidateUserId)
                .orElseGet(() -> conversationRepository.save(
                        ChatConversation.builder()
                                .candidate(candidate)
                                .complete(false)
                                .lastInteractionAt(LocalDateTime.now())
                                .build()
                ));

        // 1. Fetch current history
        List<ChatMessage> existingMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<AiChatTurn> historyTurns = new ArrayList<>();
        for (ChatMessage msg : existingMessages) {
            historyTurns.add(AiChatTurn.builder()
                    .role(msg.getRole())
                    .text(msg.getMessageText())
                    .build());
        }

        // 2. Persist candidate's message
        ChatMessage candidateMessage = ChatMessage.builder()
                .conversation(conversation)
                .role("candidate")
                .messageText(newMessage)
                .build();
        messageRepository.save(candidateMessage);

        // 3. Prepare AI Request
        AiChatMessageRequest request = AiChatMessageRequest.builder()
                .candidateId(candidate.getCandidateId())
                .conversationHistory(historyTurns)
                .newMessage(newMessage)
                .build();

        AiChatMessageResponse response = aiEngineClient.sendChatMessage(request)
                .orElse(AiChatMessageResponse.builder()
                        .botReply("Thank you for sharing. Could you tell me a bit more about your availability and notice period?")
                        .conversationComplete(false)
                        .build());

        // 4. Persist bot reply
        ChatMessage botMessage = ChatMessage.builder()
                .conversation(conversation)
                .role("bot")
                .messageText(response.getBotReply())
                .build();
        messageRepository.save(botMessage);

        conversation.setLastInteractionAt(LocalDateTime.now());
        conversation.setComplete(response.isConversationComplete());
        conversationRepository.save(conversation);

        // 5. If extractedFields are present, auto-update candidate profile
        if (response.getExtractedFields() != null) {
            updateCandidateFromExtractedFields(candidate, response.getExtractedFields());
        }

        return response;
    }

    private void updateCandidateFromExtractedFields(Candidate candidate, AiExtractedFields fields) {
        boolean updated = false;

        if (fields.getExpectedCtc() != null && !fields.getExpectedCtc().isBlank()) {
            BigDecimal ctc = parseSalaryOrCtc(fields.getExpectedCtc());
            if (ctc != null) {
                candidate.setExpectedCtc(ctc);
                updated = true;
            }
        }

        if (fields.getCurrentCtc() != null && !fields.getCurrentCtc().isBlank()) {
            BigDecimal ctc = parseSalaryOrCtc(fields.getCurrentCtc());
            if (ctc != null) {
                candidate.setCurrentCtc(ctc);
                updated = true;
            }
        }

        if (fields.getNoticePeriod() != null && !fields.getNoticePeriod().isBlank()) {
            Integer days = parseNoticeDays(fields.getNoticePeriod());
            if (days != null) {
                candidate.setNoticePeriod(days);
                updated = true;
            }
        }

        if (updated) {
            candidateRepository.save(candidate);
            log.info("Auto-updated candidate profile {} from AI Chatbot extraction", candidate.getCandidateId());
        }
    }

    private BigDecimal parseSalaryOrCtc(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                double val = Double.parseDouble(matcher.group(1));
                return BigDecimal.valueOf(val);
            }
        } catch (Exception ex) {
            log.warn("Could not parse salary string '{}': {}", text, ex.getMessage());
        }
        return null;
    }

    private Integer parseNoticeDays(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                int days = Integer.parseInt(matcher.group(1));
                if (text.toLowerCase().contains("month")) {
                    days = days * 30;
                }
                return days;
            }
            if (text.toLowerCase().contains("immediate") || text.toLowerCase().contains("serving")) {
                return 0;
            }
        } catch (Exception ex) {
            log.warn("Could not parse notice period '{}': {}", text, ex.getMessage());
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiChatTurn> getConversationHistory(UUID candidateUserId) {
        return conversationRepository.findLatestByCandidateUserId(candidateUserId)
                .map(conversation -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                        .stream()
                        .map(msg -> AiChatTurn.builder()
                                .role(msg.getRole())
                                .text(msg.getMessageText())
                                .build())
                        .toList())
                .orElse(Collections.emptyList());
    }

    @Override
    public void resetConversation(UUID candidateUserId) {
        conversationRepository.findLatestByCandidateUserId(candidateUserId)
                .ifPresent(conversation -> {
                    conversation.setComplete(true);
                    conversationRepository.save(conversation);
                });
    }
}
