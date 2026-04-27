package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddPaymentResponse {
    private String message;
    private String subscriptionNumber;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String paymentId;
    private PaymentStatus status;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    
}
