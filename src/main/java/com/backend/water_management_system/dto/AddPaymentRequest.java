package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;

public class AddPaymentRequest {
    private String subscriptionNumber;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentType paymentType;

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }  
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }

    public AddPaymentRequest() {}
}
