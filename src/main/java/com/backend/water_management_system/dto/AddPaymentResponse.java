package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;

public class AddPaymentResponse {
    private String message;
    private String subscriptionNumber;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String paymentId;
    private PaymentStatus status;
    private PaymentType paymentType;
    private LocalDateTime createdAt;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }
    public BigDecimal getOldBalance() { return oldBalance; }
    public void setOldBalance(BigDecimal oldBalance) { this.oldBalance = oldBalance; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    
}
