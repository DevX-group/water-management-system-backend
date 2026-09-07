package com.backend.water_management_system.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillResponse {
    public Long billId;
    public String billingPeriod;
    public LocalDate billDate;
    public LocalDate dueDate;
    public Integer usageUnits;
    public Integer previousReading;
    public Integer currentReading;
    public BigDecimal baseCharge;
    public BigDecimal usageCharge;
    public BigDecimal taxAmount;
    public BigDecimal totalAmount;
    public BigDecimal balanceDue;
    public String status;
    public String customerName;
    public String nic;
    public String subscriptionNumber;
}
