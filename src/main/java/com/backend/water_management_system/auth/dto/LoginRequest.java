package com.backend.water_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "NIC is required")
        String nic,

        @NotBlank(message = "Password is required")
        String password
) {}
