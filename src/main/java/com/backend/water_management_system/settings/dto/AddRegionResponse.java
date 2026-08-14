package com.backend.water_management_system.settings.dto;

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
}
