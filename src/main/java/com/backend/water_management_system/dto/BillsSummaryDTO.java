package com.backend.water_management_system.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillsSummaryDTO {

    private String customerId;
    private String customerName;
    private Double totalAmount;
    private LocalDate lastBillDate;
    private long unpaidCount;
}