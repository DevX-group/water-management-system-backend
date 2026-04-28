package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutstandingBillResponse {
    private Long billId;
    private String billingPeriod;
    private LocalDate billDate;
    private BigDecimal balanceDue;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

}
