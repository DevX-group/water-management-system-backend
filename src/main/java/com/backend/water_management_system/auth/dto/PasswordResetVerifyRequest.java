package com.backend.water_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetVerifyRequest(
        @NotBlank(message = "NIC is required")
        @Pattern(regexp = "^([0-9]{9}[vVxX]|[0-9]{12})$", message = "Please enter a valid NIC number")
        String nic,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
        String otp
) {}
