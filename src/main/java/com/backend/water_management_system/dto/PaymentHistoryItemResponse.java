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