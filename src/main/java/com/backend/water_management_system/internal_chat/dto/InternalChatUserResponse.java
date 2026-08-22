package com.backend.water_management_system.internal_chat.dto;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;

import java.util.UUID;

/** Minimal staff profile used by the internal-chat user picker. */
public record InternalChatUserResponse(
        UUID id,
        String fullName,
        Role role) {
    /** Converts an existing application user into the chat user response. */
    public static InternalChatUserResponse from(User user) {
        return new InternalChatUserResponse(
                user.getId(),
                user.getFullName(),
                user.getRole());
    }
}
