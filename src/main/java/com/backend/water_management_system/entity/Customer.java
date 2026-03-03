package com.backend.water_management_system.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String subscriptionNumber; // PK

    private String accountHolderName;
    private BigDecimal outstandingBalance;

    @ManyToOne
    @JoinColumn(name = "region_code")
    private Region region;

    // getters/setters
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public Customer() {}

    public Customer(String subscriptionNumber,
            String accountHolderName,
            Region region,
            BigDecimal outstandingBalance) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.region = region;
        this.outstandingBalance = outstandingBalance;
    }
}
