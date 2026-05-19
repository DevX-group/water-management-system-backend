package com.backend.water_management_system.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationRequest(
        @NotBlank(message = "Customer name is required")
        String accountHolderName,

        @NotBlank(message = "NIC is required")
        String nic,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @Email(message = "Invalid email format")
        String email, // Optional

        @NotBlank(message = "Connection type is required")
        String connectionType,

        @NotNull(message = "Region code is required")
        String regionCode
) {
}
