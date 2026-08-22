package com.backend.water_management_system.internal_chat.controller;

import com.backend.water_management_system.internal_chat.dto.ConversationResponse;
import com.backend.water_management_system.internal_chat.dto.CreateConversationRequest;
import com.backend.water_management_system.internal_chat.dto.InternalChatUserResponse;
import com.backend.water_management_system.internal_chat.dto.InternalChatReadReceipt;
import com.backend.water_management_system.internal_chat.dto.MessageResponse;
import com.backend.water_management_system.internal_chat.dto.SendMessageRequest;
import com.backend.water_management_system.internal_chat.service.InternalChatService;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.enums.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal-chat")
@RequiredArgsConstructor
/** Exposes the Phase 1 internal-chat REST API. */
public class InternalChatController {

    private final InternalChatService internalChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    /** Lists active staff members available to start a direct conversation with. */
    public ResponseEntity<List<InternalChatUserResponse>> getEligibleStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(internalChatService.searchEligibleUsers(principal.getUser().getId(), role, search));
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    /** Lists the authenticated user's conversations with optional filters. */
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(internalChatService.getConversationList(principal.getUser().getId(), role, search));
    }

    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    /** Creates or retrieves a direct conversation with the requested staff user. */
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.ok(internalChatService.createConversation(principal.getUser().getId(), request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    /** Returns a paginated message history for a conversation. */
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity
                .ok(internalChatService.getMessages(principal.getUser().getId(), conversationId, page, size));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    /** Marks the authenticated user's messages in a conversation as read. */
    public ResponseEntity<Void> markConversationAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        internalChatService.markConversationAsRead(principal.getUser().getId(), conversationId);
        var readerId = principal.getUser().getId();
        var recipient = internalChatService.getOtherParticipant(readerId, conversationId);
        var readAt = internalChatService.getLastReadAt(readerId, conversationId);
        messagingTemplate.convertAndSendToUser(recipient.getNic(), "/queue/internal-chat-read",
                new InternalChatReadReceipt(conversationId, readerId, readAt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    /** Stores a message sent through the REST transport. */
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = internalChatService.sendMessage(principal.getUser().getId(), conversationId,
                request);
        publishMessage(principal.getUser().getId(), conversationId, response);
        return ResponseEntity.ok(response);
    }

    private void publishMessage(UUID senderId, UUID conversationId, MessageResponse response) {
        messagingTemplate.convertAndSend("/topic/internal-chat/conversation/" + conversationId, response);
        var recipient = internalChatService.getOtherParticipant(senderId, conversationId);
        messagingTemplate.convertAndSendToUser(recipient.getNic(), "/queue/internal-chat", response);
    }
}
