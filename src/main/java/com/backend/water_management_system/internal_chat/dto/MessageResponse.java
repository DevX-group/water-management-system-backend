package com.backend.water_management_system.internal_chat.dto;

import com.backend.water_management_system.internal_chat.entity.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/** Read-only message representation returned by REST and WebSocket clients. */
public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        LocalDateTime createdAt) {
    /** Converts the persisted message entity into the public response contract. */
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt());
    }
}
