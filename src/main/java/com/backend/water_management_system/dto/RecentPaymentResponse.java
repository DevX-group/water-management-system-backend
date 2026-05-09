package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentPaymentResponse {
    private String paymentId;
    private String subscriptionNumber;
    private String accountHolderName;
    private BigDecimal amountPaid;
    private String status;
    private LocalDateTime createdAt;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;

}
