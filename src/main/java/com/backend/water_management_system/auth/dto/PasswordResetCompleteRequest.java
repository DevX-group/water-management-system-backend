package com.backend.water_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(
        @NotBlank(message = "Reset authorization is required")
        String resetAuthorization,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "Please confirm your password")
        @Size(min = 8, message = "Password confirmation must be at least 8 characters")
        String confirmPassword
) {}
