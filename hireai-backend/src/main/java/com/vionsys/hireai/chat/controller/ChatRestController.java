package com.vionsys.hireai.chat.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.storage.FileSecurityValidator;
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
    private final FileSecurityValidator fileSecurityValidator;

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
    @Operation(summary = "Upload chat attachment", description = "Validates file type, size, and magic bytes before storing outside web root")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAttachment(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // 1. Authorize: verify user is a participant in this conversation
        chatService.getConversationById(currentUser.getId(), conversationId);

        // 2. Validate: type, size, magic bytes, script patterns, and ImageIO decoding
        fileSecurityValidator.validateChatAttachment(file);

        // 3. Store: isolated in dedicated attachments folder with random UUID filename and non-executable permissions
        String storedFileName = fileStorageService.storeAttachment(file);

        // 4. Detect true MIME type
        String detectedMime = fileSecurityValidator.detectMimeType(file, fileSecurityValidator.readHeaderBytes(file, 2048));

        // 5. Construct safe download URL within the application context
        String attachmentUrl = "/chat/conversations/" + conversationId + "/attachments/" + storedFileName;

        Map<String, Object> fileData = Map.of(
                "url", attachmentUrl,
                "name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                "size", file.getSize(),
                "contentType", detectedMime
        );
        return ResponseEntity.ok(ApiResponse.success(fileData, "Attachment uploaded successfully"));
    }

    @GetMapping("/conversations/{conversationId}/attachments/{filename}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download or view chat attachment", description = "Serves chat attachment with strict security headers")
    public ResponseEntity<Resource> downloadAttachment(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID conversationId,
            @PathVariable String filename
    ) throws IOException {
        // 1. Authorize: verify user is a participant in this conversation
        chatService.getConversationById(currentUser.getId(), conversationId);

        // 2. Validate filename to prevent path traversal
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..") || filename.contains("\0")) {
            throw new FileStorageException("Invalid attachment path request.");
        }

        // 3. Load resource safely
        Resource resource = fileStorageService.loadAttachmentAsResource(filename);

        // 4. Determine content type and safe disposition
        String lower = filename.toLowerCase(Locale.ROOT);
        MediaType mediaType;
        ContentDisposition contentDisposition;

        if (lower.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
            contentDisposition = ContentDisposition.inline().filename(filename).build();
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
            contentDisposition = ContentDisposition.inline().filename(filename).build();
        } else if (lower.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
            contentDisposition = ContentDisposition.inline().filename(filename).build();
        } else if (lower.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
            contentDisposition = ContentDisposition.inline().filename(filename).build();
        } else if (lower.endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
            contentDisposition = ContentDisposition.attachment().filename(filename).build();
        } else if (lower.endsWith(".docx")) {
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            contentDisposition = ContentDisposition.attachment().filename(filename).build();
        } else if (lower.endsWith(".doc")) {
            mediaType = MediaType.parseMediaType("application/msword");
            contentDisposition = ContentDisposition.attachment().filename(filename).build();
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
            contentDisposition = ContentDisposition.attachment().filename(filename).build();
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
