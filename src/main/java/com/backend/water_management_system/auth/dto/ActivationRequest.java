package com.backend.water_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Sent by the user when they click the activation link and set their password
public record ActivationRequest(
        @NotBlank(message = "Activation token is required")
        String token,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword
) {}
