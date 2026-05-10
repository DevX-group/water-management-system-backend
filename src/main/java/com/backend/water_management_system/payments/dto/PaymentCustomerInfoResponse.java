package com.backend.water_management_system.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCustomerInfoResponse {
    private String subscriptionNumber;
    private String accountHolderName;
    private String region;
    private String connectionType;
    private String nic;

}
