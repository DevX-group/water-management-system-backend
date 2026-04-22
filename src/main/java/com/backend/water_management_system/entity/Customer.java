package com.backend.water_management_system.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String nic;
    private String email;
    private String address;
    private String phoneNumber;
    
    @ManyToOne
    @JoinColumn(name = "region_code")
    private Region region;
    
}
