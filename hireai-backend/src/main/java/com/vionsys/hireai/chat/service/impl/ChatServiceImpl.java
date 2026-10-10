package com.vionsys.hireai.chat.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.chat.dto.request.CreateConversationRequest;
import com.vionsys.hireai.chat.dto.request.SendMessageRequest;
import com.vionsys.hireai.chat.dto.response.ChatMessageResponse;
import com.vionsys.hireai.chat.dto.response.ConversationSummaryResponse;
import com.vionsys.hireai.chat.dto.response.UnreadCountResponse;
import com.vionsys.hireai.chat.entity.DirectChatMessage;
import com.vionsys.hireai.chat.entity.DirectChatParticipant;
import com.vionsys.hireai.chat.entity.DirectConversation;
import com.vionsys.hireai.chat.enums.MessageStatus;
import com.vionsys.hireai.chat.enums.MessageType;
import com.vionsys.hireai.chat.mapper.ChatMapper;
import com.vionsys.hireai.chat.repository.DirectChatMessageRepository;
import com.vionsys.hireai.chat.repository.DirectChatParticipantRepository;
import com.vionsys.hireai.chat.repository.DirectConversationRepository;
import com.vionsys.hireai.chat.service.ChatService;
import com.vionsys.hireai.common.dto.PagedResponse;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DirectConversationRepository conversationRepository;
    private final DirectChatMessageRepository chatMessageRepository;
    private final DirectChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.vionsys.hireai.notification.service.NotificationService notificationService;

    @Override
    @Transactional
    public ConversationSummaryResponse getOrCreateConversation(UUID currentUserId, CreateConversationRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Resolve candidate user (either direct User ID or Candidate profile ID)
        User candidateUser = userRepository.findById(request.getCandidateId())
                .orElseGet(() -> candidateRepository.findById(request.getCandidateId())
                        .map(c -> c.getUser())
                        .orElseThrow(() -> new UserNotFoundException("Target candidate not found")));

        // Identify recruiter and candidate
        User recruiterUser;
        User targetCandidateUser;

        if (currentUser.getRole() != null && !currentUser.getRole().getName().name().contains("CANDIDATE")) {
            recruiterUser = currentUser;
            targetCandidateUser = candidateUser;
        } else {
            recruiterUser = candidateUser;
            targetCandidateUser = currentUser;
        }

        // Check for existing conversation
        List<DirectConversation> existingList = conversationRepository
                .findExistingConversations(recruiterUser.getId(), targetCandidateUser.getId(), request.getJobId());

        DirectConversation conversation = existingList.isEmpty() ? null : existingList.get(0);

        if (conversation == null) {
            Job job = null;
            if (request.getJobId() != null) {
                job = jobRepository.findById(request.getJobId()).orElse(null);
            }

            JobApplication jobApplication = null;
            if (request.getJobApplicationId() != null) {
                jobApplication = jobApplicationRepository.findById(request.getJobApplicationId()).orElse(null);
            }

            conversation = DirectConversation.builder()
                    .recruiter(recruiterUser)
                    .candidate(targetCandidateUser)
                    .job(job)
                    .jobApplication(jobApplication)
                    .isActive(true)
                    .build();

            conversation = conversationRepository.save(conversation);

            // Create participant entries
            DirectChatParticipant recruiterParticipant = DirectChatParticipant.builder()
                    .conversation(conversation)
                    .user(recruiterUser)
                    .unreadCount(0)
                    .build();

            DirectChatParticipant candidateParticipant = DirectChatParticipant.builder()
                    .conversation(conversation)
                    .user(targetCandidateUser)
                    .unreadCount(0)
                    .build();

            chatParticipantRepository.save(recruiterParticipant);
            chatParticipantRepository.save(candidateParticipant);

            // If initial message provided, send it
            if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
                sendMessage(currentUserId, SendMessageRequest.builder()
                        .conversationId(conversation.getId())
                        .recipientId(targetCandidateUser.getId().equals(currentUserId) ? recruiterUser.getId() : targetCandidateUser.getId())
                        .messageText(request.getInitialMessage().trim())
                        .messageType(MessageType.TEXT)
                        .build());
            }
        }

        DirectChatParticipant participant = chatParticipantRepository
                .findByConversationIdAndUserId(conversation.getId(), currentUserId)
                .orElse(null);

        Integer unread = participant != null ? participant.getUnreadCount() : 0;
        return ChatMapper.toConversationSummary(conversation, currentUserId, unread, false);
    }

    @Override
    @Transactional
    public List<ConversationSummaryResponse> getUserConversations(UUID currentUserId) {
        List<DirectConversation> conversations = conversationRepository.findAllByUserId(currentUserId);

        // If user has no conversations in DB yet, auto-link with active counterpart in DB
        if (conversations.isEmpty()) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                boolean isCandidate = currentUser.getRole() != null && currentUser.getRole().getName().name().contains("CANDIDATE");
                if (isCandidate) {
                    userRepository.findByEmail("jordan.kim@vionsys.com").ifPresent(recruiter -> {
                        getOrCreateConversation(currentUserId, CreateConversationRequest.builder()
                                .candidateId(currentUserId)
                                .initialMessage("Hi Jordan! I am excited to connect regarding opportunities on HireAI.")
                                .build());
                    });
                } else {
                    userRepository.findByEmail("alex.rivera@example.com").ifPresent(cand -> {
                        getOrCreateConversation(currentUserId, CreateConversationRequest.builder()
                                .candidateId(cand.getId())
                                .initialMessage("Hi Alex! We would love to discuss exciting opportunities with you.")
                                .build());
                    });
                }
                conversations = conversationRepository.findAllByUserId(currentUserId);
            }
        }

        return conversations.stream().map(c -> {
            DirectChatParticipant participant = chatParticipantRepository
                    .findByConversationIdAndUserId(c.getId(), currentUserId)
                    .orElse(null);
            Integer unread = participant != null ? participant.getUnreadCount() : 0;
            return ChatMapper.toConversationSummary(c, currentUserId, unread, false);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationSummaryResponse getConversationById(UUID currentUserId, UUID conversationId) {
        DirectConversation conversation = conversationRepository.findByIdWithDetails(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(conversation, currentUserId);

        DirectChatParticipant participant = chatParticipantRepository
                .findByConversationIdAndUserId(conversation.getId(), currentUserId)
                .orElse(null);
        Integer unread = participant != null ? participant.getUnreadCount() : 0;

        return ChatMapper.toConversationSummary(conversation, currentUserId, unread, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getConversationMessages(
            UUID currentUserId,
            UUID conversationId,
            Pageable pageable
    ) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(conversation, currentUserId);

        Page<DirectChatMessage> messagePage = chatMessageRepository.findByConversationId(conversationId, pageable);
        Page<ChatMessageResponse> responsePage = messagePage.map(ChatMapper::toChatMessageResponse);

        return PagedResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        DirectConversation conversation = conversationRepository.findByIdWithDetails(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(conversation, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found"));

        User recipient = null;
        if (request.getRecipientId() != null) {
            recipient = userRepository.findById(request.getRecipientId()).orElse(null);
        }
        if (recipient == null) {
            recipient = conversation.getRecruiter().getId().equals(senderId)
                    ? conversation.getCandidate()
                    : conversation.getRecruiter();
        }

        // Build and save message
        DirectChatMessage message = DirectChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .recipient(recipient)
                .messageText(request.getMessageText())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .attachmentSize(request.getAttachmentSize())
                .metadataJson(request.getMetadataJson())
                .status(MessageStatus.SENT)
                .build();

        message = chatMessageRepository.save(message);

        // Update conversation summary
        String previewText = message.getMessageText();
        if (previewText == null && message.getMessageType() == MessageType.ATTACHMENT) {
            previewText = "📎 " + (message.getAttachmentName() != null ? message.getAttachmentName() : "Attachment");
        } else if (previewText == null && message.getMessageType() == MessageType.INTERVIEW_INVITE) {
            previewText = "📅 Interview Invitation";
        }

        conversation.setLastMessageText(previewText);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Increment recipient unread badge
        chatParticipantRepository.incrementUnreadCount(conversation.getId(), recipient.getId());

        ChatMessageResponse response = ChatMapper.toChatMessageResponse(message);

        // Push real-time event to recipient user queue
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/messages",
                    response
            );
        } catch (Exception e) {
            log.warn("[ChatService] Failed to push WebSocket message to recipient {}: {}", recipient.getEmail(), e.getMessage());
        }

        // Also push in-app real-time notification to recipient's notification bell
        try {
            if (recipient != null) {
                String senderName = sender.getFirstName() + (sender.getLastName() != null ? " " + sender.getLastName() : "");
                boolean isRecipientRecruiter = recipient.getRole() != null && recipient.getRole().getName() != null &&
                        recipient.getRole().getName().name().contains("RECRUITER");
                String chatRoute = isRecipientRecruiter ? "/recruiter/inbox" : "/candidate/inbox";

                if (message.getMessageType() == MessageType.INTERVIEW_INVITE) {
                    notificationService.createAndSendNotification(
                            recipient,
                            "Interview Invitation Received 📅",
                            senderName + " sent you an interview invitation. View your inbox to confirm your slot.",
                            com.vionsys.hireai.notification.enums.NotificationType.INTERVIEW,
                            chatRoute
                    );
                } else {
                    notificationService.createAndSendNotification(
                            recipient,
                            "New Message from " + senderName,
                            previewText != null ? previewText : "Sent you a message",
                            com.vionsys.hireai.notification.enums.NotificationType.CHAT,
                            chatRoute
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("[ChatService] Failed to dispatch chat notification: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID currentUserId, UUID conversationId) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(conversation, currentUserId);

        // Update messages status to READ
        chatMessageRepository.markMessagesAsRead(
                conversationId,
                currentUserId,
                MessageStatus.READ,
                LocalDateTime.now()
        );

        // Reset unread count for current user
        chatParticipantRepository.resetUnreadCount(conversationId, currentUserId);

        // Notify partner that messages have been read
        User partner = conversation.getRecruiter().getId().equals(currentUserId)
                ? conversation.getCandidate()
                : conversation.getRecruiter();

        try {
            messagingTemplate.convertAndSendToUser(
                    partner.getEmail(),
                    "/queue/read-receipt",
                    conversationId.toString()
            );
        } catch (Exception e) {
            log.warn("[ChatService] Failed to push read-receipt: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getTotalUnreadCount(UUID currentUserId) {
        long count = chatMessageRepository.countTotalUnreadByRecipientId(currentUserId);
        return UnreadCountResponse.builder().totalUnread(count).build();
    }

    private void validateParticipant(DirectConversation conversation, UUID userId) {
        boolean isRecruiter = conversation.getRecruiter() != null && conversation.getRecruiter().getId().equals(userId);
        boolean isCandidate = conversation.getCandidate() != null && conversation.getCandidate().getId().equals(userId);

        if (!isRecruiter && !isCandidate) {
            throw new AccessDeniedException("User is not authorized to access this conversation");
        }
    }
}
