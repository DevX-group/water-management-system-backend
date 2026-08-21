package com.backend.water_management_system.internal_chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for creating or retrieving a direct conversation with another
 * staff user.
 */
public record CreateConversationRequest(
        @NotNull(message = "Target user ID is required.") UUID targetUserId) {
}
