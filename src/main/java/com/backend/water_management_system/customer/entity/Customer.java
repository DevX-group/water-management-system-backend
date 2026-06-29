package com.backend.water_management_system.customer.entity;

import java.math.BigDecimal;

import com.backend.water_management_system.common.entity.Region;

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
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private com.backend.water_management_system.user.entity.User user;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private String connectionType; // "metered" or "non-metered"
    
    private BigDecimal outstandingBalance;
    
    @ManyToOne
    @JoinColumn(name = "region_code")
    private Region region;

    public Customer(String subscriptionNumber, String accountHolderName, com.backend.water_management_system.user.entity.User user,
            String address, String connectionType, Region region) {
        this.subscriptionNumber = subscriptionNumber;
        this.accountHolderName = accountHolderName;
        this.user = user;
        this.address = address;
        this.connectionType = connectionType;
        this.region = region;
    }

    public String getNic() {
        return user != null ? user.getNic() : null;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getMobileNumber() {
        return user != null ? user.getPhoneNumber() : null;
    }
}
