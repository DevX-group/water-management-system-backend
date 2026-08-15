package com.backend.water_management_system.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerProfileUpdateRequest(
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,
        
        @Email(message = "Invalid email format")
        String email,
        
        @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
        String phoneNumber
) {
}
