package com.backend.water_management_system.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.backend.water_management_system.payments.enums.SlipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBankSlipResponse {
    private Long slipId;
    private String subscriptionNumber;
    private String accountHolderName;
    private BigDecimal amount;
    private String bankReference;
    private String filePath;
    private SlipStatus status;
    private LocalDate bankPaymentDate;
    private LocalDateTime uploadedAt;
}
