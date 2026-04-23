package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import com.backend.water_management_system.entity.Region;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPaymentResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;

    private String merchantId;
    private String orderId;
    private String items;

    private String currency;
    private BigDecimal amount;

    private String returnUrl;
    private String cancelUrl;
    private String notifyUrl;

    private String hash;
}
