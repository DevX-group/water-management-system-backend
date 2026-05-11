package com.backend.water_management_system.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSearchResponse {
    private String subscriptionNumber;
    private String accountHolderName;

}
