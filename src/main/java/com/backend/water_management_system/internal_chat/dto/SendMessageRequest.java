package com.backend.water_management_system.internal_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** REST request body containing the text of a new internal-chat message. */
public record SendMessageRequest(
        @NotBlank(message = "Message content is required.") @Size(max = 2000, message = "Message content must be at most 2000 characters.") String content) {
}
