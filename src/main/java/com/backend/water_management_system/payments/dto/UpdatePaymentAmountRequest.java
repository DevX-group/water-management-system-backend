package com.backend.water_management_system.payments.dto;

import java.math.BigDecimal;

public class UpdatePaymentAmountRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal newAmount) {
        this.amount = newAmount;
    }
}
