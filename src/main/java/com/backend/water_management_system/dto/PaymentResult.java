package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import com.backend.water_management_system.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResult {
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private PaymentStatus status;
}
