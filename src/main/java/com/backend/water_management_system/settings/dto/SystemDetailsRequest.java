package com.backend.water_management_system.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemDetailsRequest {
    private String companyName;
    private String officeAddress;
    private String officeContactNumber;
    private String officeEmail;
    private String defaultCurrency;
}
