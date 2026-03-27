package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecentPaymentResponse {
    private String subscriptionNumber;
    private String accountHolderName;
    private BigDecimal amountPaid;
    private String status;
    private LocalDateTime createdAt;

    // Constructors
    public RecentPaymentResponse() {
    }

    public RecentPaymentResponse(String subscriptionNumber, String accountHolderName, BigDecimal amountPaid, String status, LocalDateTime createdAt) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.amountPaid = amountPaid;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public void setSubscriptionNumber(String subscriptionNumber) {
        this.subscriptionNumber = subscriptionNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
