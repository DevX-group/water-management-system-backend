package com.backend.water_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankDetailsResponse {
    private String bankName;
    private String branch;
    private String accountNumber;
    private String accountName;
}
