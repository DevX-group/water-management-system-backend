package com.backend.water_management_system.settings.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRegionResponse {
    
    private String regionCode; 
    private String regionName;
    private BigDecimal baseRate;
    private BigDecimal unitRateTier1;
    private BigDecimal unitRateTier2;
    private BigDecimal unitRateTier3;
    private BigDecimal taxRate;
}
