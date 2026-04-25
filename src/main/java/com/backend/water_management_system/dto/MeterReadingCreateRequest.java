package com.backend.water_management_system.dto;
import java.time.LocalDate;
public class MeterReadingCreateRequest {
    public String meterNumber;
    public String subscriptionNumber;
    public Integer previousReading;
    public Integer currentReading;
    public Integer usageUnits;
    public LocalDate readingDate;
    public String notes;
    public Long submittedBy; // admin id (optional)
}