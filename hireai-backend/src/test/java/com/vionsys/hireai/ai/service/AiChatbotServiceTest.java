package com.vionsys.hireai.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.chatbot.AiExtractedFields;
import com.vionsys.hireai.ai.entity.ChatConversation;
import com.vionsys.hireai.ai.repository.ChatConversationRepository;
import com.vionsys.hireai.ai.repository.ChatMessageRepository;
import com.vionsys.hireai.ai.service.impl.AiChatbotServiceImpl;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;

@ExtendWith(MockitoExtension.class)
class AiChatbotServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private AiEngineClient aiEngineClient;

    @InjectMocks
    private AiChatbotServiceImpl chatbotService;

    private UUID userId;
    private Candidate candidate;
    private ChatConversation conversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-202")
                .firstName("Sarah")
                .lastName("Connor")
                .build();

        conversation = ChatConversation.builder()
                .id(UUID.randomUUID())
                .candidate(candidate)
                .complete(false)
                .build();
    }

    @Test
    void testSendMessage_AndAutoExtractFields() {
        AiExtractedFields extracted = AiExtractedFields.builder()
                .expectedCtc("15 LPA")
                .noticePeriod("30 days")
                .build();

        AiChatMessageResponse aiResponse = AiChatMessageResponse.builder()
                .botReply("Understood. Thank you for providing your expectations!")
                .extractedFields(extracted)
                .conversationComplete(true)
                .build();

        when(candidateRepository.findByUserId(userId)).thenReturn(Optional.of(candidate));
        when(conversationRepository.findLatestByCandidateUserId(userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())).thenReturn(Collections.emptyList());
        when(aiEngineClient.sendChatMessage(any())).thenReturn(Optional.of(aiResponse));

        AiChatMessageResponse result = chatbotService.sendMessage(userId, "I am looking for 15 LPA and my notice period is 30 days.");

        assertNotNull(result);
        assertEquals("Understood. Thank you for providing your expectations!", result.getBotReply());
        verify(candidateRepository).save(candidate);
        assertEquals(BigDecimal.valueOf(15.0), candidate.getExpectedCtc());
        assertEquals(30, candidate.getNoticePeriod());
    }
}
