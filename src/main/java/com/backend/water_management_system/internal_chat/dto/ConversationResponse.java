package com.backend.water_management_system.internal_chat.dto;

import com.backend.water_management_system.internal_chat.entity.Conversation;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model for a conversation list item, including its latest-message preview
 * and unread count.
 */
public record ConversationResponse(
        UUID id,
        UUID otherParticipantId,
        String otherParticipantName,
        Role otherParticipantRole,
        String latestMessagePreview,
        LocalDateTime latestMessageTime,
        long unreadCount) {
    /** Builds the API response from the conversation and its other participant. */
    public static ConversationResponse from(Conversation conversation, User otherParticipant,
            String latestMessagePreview,
            LocalDateTime latestMessageTime, long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                otherParticipant.getId(),
                otherParticipant.getFullName(),
                otherParticipant.getRole(),
                latestMessagePreview,
                latestMessageTime,
                unreadCount);
    }
}
