package com.backend.water_management_system.dto;

import java.math.BigDecimal;

public class MeterReadingCreateResponse {
    public Long readingId;
    public Integer usageUnits;
    public Long billId;
    public BigDecimal totalAmount;
    public String status;
}

