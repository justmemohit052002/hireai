package com.vionsys.hireai.chat.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.storage.FileStorageService;
import com.vionsys.hireai.chat.dto.request.CreateConversationRequest;
import com.vionsys.hireai.chat.dto.request.SendMessageRequest;
import com.vionsys.hireai.chat.dto.response.ChatMessageResponse;
import com.vionsys.hireai.chat.dto.response.ConversationSummaryResponse;
import com.vionsys.hireai.chat.dto.response.UnreadCountResponse;
import com.vionsys.hireai.chat.service.ChatService;
import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.common.dto.PagedResponse;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Recruiter-Candidate Real-Time Chat", description = "Endpoints for managing conversations, message history, and chat attachments")
public class ChatRestController {

    private final ChatService chatService;
    private final FileStorageService fileStorageService;

    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get or create a conversation thread", description = "Finds existing conversation or initializes a new chat thread for a recruiter and candidate")
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> getOrCreateConversation(
            @CurrentUser CustomUserDetails currentUser,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        ConversationSummaryResponse response = chatService.getOrCreateConversation(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response, "Conversation initialized successfully"));
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all conversations for logged in user", description = "Returns active conversation threads sorted by last message time")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getUserConversations(
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<ConversationSummaryResponse> conversations = chatService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(conversations, "Conversations retrieved successfully"));
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conversation details by ID")
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> getConversationById(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId
    ) {
        ConversationSummaryResponse response = chatService.getConversationById(currentUser.getId(), conversationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversation details retrieved"));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated message history for a conversation")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getConversationMessages(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<ChatMessageResponse> messages = chatService.getConversationMessages(currentUser.getId(), conversationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages, "Messages retrieved successfully"));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Send a message via REST fallback")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        request.setConversationId(conversationId);
        ChatMessageResponse response = chatService.sendMessage(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Message sent successfully"));
    }

    @PatchMapping("/conversations/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all unread messages in conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId
    ) {
        chatService.markConversationAsRead(currentUser.getId(), conversationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Messages marked as read"));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get total unread messages count for navbar badge")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @CurrentUser CustomUserDetails currentUser
    ) {
        UnreadCountResponse count = chatService.getTotalUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(count, "Unread count retrieved"));
    }

    @PostMapping(value = "/conversations/{conversationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload chat attachment", description = "Uploads file and returns URL/metadata for chat message")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAttachment(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String storedPath = fileStorageService.store(file);
        Map<String, Object> fileData = Map.of(
                "url", "/uploads/" + storedPath,
                "name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                "size", file.getSize(),
                "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
        return ResponseEntity.ok(ApiResponse.success(fileData, "Attachment uploaded successfully"));
    }
}
