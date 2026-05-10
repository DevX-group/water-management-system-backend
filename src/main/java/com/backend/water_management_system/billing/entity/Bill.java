package com.backend.water_management_system.billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.MeterReading;

@Entity
@Table(name="bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billId;

    @ManyToOne
    @JoinColumn(name="subscription_number")
    private Customer customer;

    private String billingPeriod; // e.g. "2026-02"
    private LocalDate billDate;
    private LocalDate dueDate;

    private Integer usageUnits;

    private BigDecimal baseCharge;
    private BigDecimal usageCharge;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private BigDecimal balanceDue;

    private BigDecimal outstandingAtIssue;

    private String status; // PENDING/PAID/OVERDUE
    private OffsetDateTime generatedAt;

    @OneToOne
    @JoinColumn(name="reading_id")
    private MeterReading meterReading;

    // getters/setters
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getUsageUnits() { return usageUnits; }
    public void setUsageUnits(Integer usageUnits) { this.usageUnits = usageUnits; }
    public BigDecimal getBaseCharge() { return baseCharge; }
    public void setBaseCharge(BigDecimal baseCharge) { this.baseCharge = baseCharge; }
    public BigDecimal getUsageCharge() { return usageCharge; }
    public void setUsageCharge(BigDecimal usageCharge) { this.usageCharge = usageCharge; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }
    public BigDecimal getOutstandingAtIssue() { return outstandingAtIssue; }
    public void setOutstandingAtIssue(BigDecimal outstandingAtIssue) { this.outstandingAtIssue = outstandingAtIssue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
    public MeterReading getMeterReading() { return meterReading; }
    public void setMeterReading(MeterReading meterReading) { this.meterReading = meterReading; }
}

