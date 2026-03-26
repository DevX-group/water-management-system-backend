package com.backend.water_management_system.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String subscriptionNumber; // PK

    private String accountHolderName;
    
    @Column(unique = true, nullable = false)
    private String nic; // National Identity Card number
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String mobileNumber;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private String connectionType; // "metered" or "non-metered"
    
    private BigDecimal outstandingBalance;

    @ManyToOne
    @JoinColumn(name = "region_code")
    private Region region;
    
    @Column(name = "account_status", nullable = false)
    private String accountStatus = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer() {}

    public Customer(String subscriptionNumber,
            String accountHolderName,
            String nic,
            String email,
            String mobileNumber,
            String address,
            String connectionType,
            Region region) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.nic = nic;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.connectionType = connectionType;
        this.region = region;
        this.outstandingBalance = BigDecimal.ZERO;
        this.accountStatus = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }
}
