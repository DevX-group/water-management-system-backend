package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPaymentRequest {
    private String subscriptionNumber;
    private BigDecimal amount;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
}
