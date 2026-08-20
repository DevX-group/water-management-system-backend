package com.backend.water_management_system.internal_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** STOMP payload identifying the conversation and carrying the message text. */
public record InternalChatWebSocketMessageRequest(
        @NotNull(message = "Conversation ID is required.") UUID conversationId,

        @NotBlank(message = "Message content is required.") @Size(max = 2000, message = "Message content must be at most 2000 characters.") String content) {
}
