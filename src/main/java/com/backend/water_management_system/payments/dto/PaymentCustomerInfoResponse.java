package com.backend.water_management_system.payments.dto;

public class PaymentCustomerInfoResponse {
    private String subscriptionNumber;
    private String accountHolderName;
    private String region;
    private String nic;

    public PaymentCustomerInfoResponse() {}

    public PaymentCustomerInfoResponse(String subscriptionNumber, String accountHolderName, String region, String nic) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.region = region;
        this.nic = nic;
    }

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

}
