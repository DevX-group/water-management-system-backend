package com.backend.water_management_system.reports.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "usage_records")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    private String area;

    private double usage;

    private double amount;

    @Column(name = "record_date")
    private LocalDate recordDate;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public double getUsage() { return usage; }
    public void setUsage(double usage) { this.usage = usage; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}
