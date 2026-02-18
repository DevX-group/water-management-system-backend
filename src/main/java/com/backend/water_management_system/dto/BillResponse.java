package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillResponse {
    public Long billId;
    public String billingPeriod;
    public LocalDate billDate;
    public LocalDate dueDate;
    public Integer usageUnits;
    public BigDecimal totalAmount;
    public BigDecimal balanceDue;
    public String status;
}
