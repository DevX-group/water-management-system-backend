package com.backend.water_management_system.internal_chat.controller;

import com.backend.water_management_system.internal_chat.dto.InternalChatWebSocketMessageRequest;
import com.backend.water_management_system.internal_chat.dto.MessageResponse;
import com.backend.water_management_system.internal_chat.dto.SendMessageRequest;
import com.backend.water_management_system.internal_chat.service.InternalChatService;
import com.backend.water_management_system.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
/** Handles live internal-chat messages received over STOMP. */
public class InternalChatWebSocketController {

    private final InternalChatService internalChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/internal-chat/send")
    /**
     * Persists a STOMP message and broadcasts the saved response to the
     * conversation topic.
     */
    public void sendMessage(@Payload @Valid InternalChatWebSocketMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new IllegalArgumentException("Unauthenticated WebSocket connection.");
        }

        // REST and WebSocket messages share the same service validation and
        // authorization path.
        SendMessageRequest contentRequest = new SendMessageRequest(request.content());
        MessageResponse response = internalChatService.sendMessage(principal.getUser().getId(),
                request.conversationId(), contentRequest);
        messagingTemplate.convertAndSend("/topic/internal-chat/conversation/" + request.conversationId(), response);
    }
}
