package com.backend.water_management_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name="customers")
public class Customer {
    @Id
    private String subscriptionNumber; // PK

    private String accountHolderName;

    @ManyToOne
    @JoinColumn(name="region_code")
    private Region region;

    // getters/setters
    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
}

