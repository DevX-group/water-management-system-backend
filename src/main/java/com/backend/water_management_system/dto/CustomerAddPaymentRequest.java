package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import com.backend.water_management_system.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAddPaymentRequest {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    private PaymentMethod paymentMethod;
}
