package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentHistoryItemResponse {
    private String paymentId;
    private String subscriptionNumber;
    private BigDecimal amount;
    private String status;      
    private String paymentType; 
    private LocalDateTime createdAt;

    public PaymentHistoryItemResponse() {}

    public PaymentHistoryItemResponse(String paymentId, String subscriptionNumber, BigDecimal amount,
                                      String status, String paymentType, LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.subscriptionNumber = subscriptionNumber;
        this.amount = amount;
        this.status = status;
        this.paymentType = paymentType;
        this.createdAt = createdAt;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}