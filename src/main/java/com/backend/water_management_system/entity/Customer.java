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
    private String nic;

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

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public Customer() {}

    public Customer(String subscriptionNumber,
            String accountHolderName,
            Region region,
            BigDecimal outstandingBalance,
            String nic) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.region = region;
        this.outstandingBalance = outstandingBalance;
        this.nic = nic;
    }
}
