package com.backend.water_management_system.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryItemResponse {
    
    private String paymentId;
    private String subscriptionNumber;
    private BigDecimal amount;
    private PaymentStatus status;      
    private PaymentType paymentType; 
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;

}