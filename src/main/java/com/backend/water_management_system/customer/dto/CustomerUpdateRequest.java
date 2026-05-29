package com.backend.water_management_system.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerUpdateRequest(
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,
        
        @NotBlank(message = "NIC is required")
        String nic,
        
        @NotBlank(message = "Address is required")
        String address,
        
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
        String phoneNumber,
        
        String email,
        
        @NotBlank(message = "Connection type is required")
        @Pattern(regexp = "^(METERED|NON_METERED)$", message = "Connection type must be METERED or NON_METERED")
        String connectionType,
        
        @NotBlank(message = "Region code is required")
        String regionCode
) {
}
