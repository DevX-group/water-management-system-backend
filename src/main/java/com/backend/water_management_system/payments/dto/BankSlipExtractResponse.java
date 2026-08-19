package com.backend.water_management_system.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankSlipExtractResponse {
    private BigDecimal amount;
    private LocalDate bankPaymentDate;
    private String bankReference;
    private boolean extracted;
    private String message;
}
