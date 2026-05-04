package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutstandingBillsSummaryResponse {
    List<OutstandingBillResponse> outstandingBills;
    BigDecimal totalOutstandingAmount;
}
