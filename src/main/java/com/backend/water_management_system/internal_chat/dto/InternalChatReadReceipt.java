package com.backend.water_management_system.internal_chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event sent to the other participant when a conversation is marked as read.
 */
public record InternalChatReadReceipt(
        UUID conversationId,
        UUID readerId,
        LocalDateTime readAt) {
}