package com.backend.water_management_system.dto;

public class CustomerSearchResponse {
    private String subscriptionNumber;
    private String accountHolderName;

    // Constructors
    public CustomerSearchResponse() {
    }

    public CustomerSearchResponse(String subscriptionNumber, String accountHolderName) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
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
}
