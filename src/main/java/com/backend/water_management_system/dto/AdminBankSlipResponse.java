package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.entity.SlipStatus;

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
    private PaymentType paymentType;
    private BigDecimal amount;
    private String bankReference;
    private String filePath;
    private SlipStatus status;
    private LocalDateTime uploadedAt;
}
