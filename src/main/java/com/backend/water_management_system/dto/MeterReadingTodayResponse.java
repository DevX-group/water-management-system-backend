package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MeterReadingTodayResponse {
    public Long readingId;
    public String meterNumber;
    public String customerName;
    public String subscriptionNumber;
    public Integer previousReading;
    public Integer currentReading;
    public Integer usageUnits;
    public LocalDate readingDate;
    public Long billId;
    public BigDecimal totalAmount;
    public String billStatus;
}