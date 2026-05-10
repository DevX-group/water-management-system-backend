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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerBankSlipResponse {
    private Long slipId;
    private BigDecimal amount;
    private String bankReference;
    private String filePath;
    private SlipStatus status;
    private LocalDateTime uploadedAt;
    private LocalDate bankPaymentDate;
    private LocalDateTime reviewedAt;
    private String rejectionReason; // Include reason if rejected
}
