package com.vionsys.hireai.chat.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.vionsys.hireai.chat.dto.request.SendMessageRequest;
import com.vionsys.hireai.chat.dto.request.TypingNotificationRequest;
import com.vionsys.hireai.chat.dto.response.ChatMessageResponse;
import com.vionsys.hireai.chat.service.ChatService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload SendMessageRequest request, Principal principal) {
        UUID senderId = extractUserId(principal);
        log.info("[WebSocket] Message received from user {} for conversation {}", senderId, request.getConversationId());
        
        ChatMessageResponse response = chatService.sendMessage(senderId, request);
        
        // Echo message back to sender matching principal username (email)
        if (principal != null && principal.getName() != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/messages",
                    response
            );
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTypingNotification(@Payload TypingNotificationRequest request, Principal principal) {
        UUID senderId = extractUserId(principal);
        log.debug("[WebSocket] Typing status from {}: {}", senderId, request.isTyping());

        // Forward typing event to recipient's email destination
        userRepository.findById(request.getRecipientId()).ifPresent(recipient -> {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/typing",
                    request
            );
        });
    }

    @MessageMapping("/chat.read")
    public void handleMarkRead(@Payload UUID conversationId, Principal principal) {
        UUID userId = extractUserId(principal);
        log.debug("[WebSocket] User {} marked conversation {} as read", userId, conversationId);
        chatService.markConversationAsRead(userId, conversationId);
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }
        }
        if (principal != null && principal.getName() != null) {
            return userRepository.findByEmail(principal.getName())
                    .map(com.vionsys.hireai.user.entity.User::getId)
                    .orElseThrow(() -> new IllegalStateException("User not found for email: " + principal.getName()));
        }
        throw new IllegalStateException("Unable to resolve authenticated user ID from WebSocket session");
    }
}
