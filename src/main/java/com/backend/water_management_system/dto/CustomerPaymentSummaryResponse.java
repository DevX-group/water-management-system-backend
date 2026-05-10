package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPaymentSummaryResponse {
    private String subscriptionNumber;
    private BigDecimal monthlyDue;
    private BigDecimal outstandingBalance;
    private BigDecimal totalDue;
    private String billStatus;
    
}
