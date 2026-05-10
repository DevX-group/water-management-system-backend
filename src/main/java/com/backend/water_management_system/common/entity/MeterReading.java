package com.backend.water_management_system.common.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

import com.backend.water_management_system.customer.entity.Customer;

@Entity
@Table(name="meter_readings")
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long readingId;

    private String meterNumber;

    @ManyToOne
    @JoinColumn(name="subscription_number")
    private Customer customer;

    private Integer previousReading;
    private Integer currentReading;
    private Integer usageUnits;
    private LocalDate readingDate;

    @Column(length = 500)
    private String notes;

    // store who submitted (admin id) - optional for now
    private Long submittedBy;

    // getters/setters
    public Long getReadingId() { return readingId; }
    public void setReadingId(Long readingId) { this.readingId = readingId; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Integer getPreviousReading() { return previousReading; }
    public void setPreviousReading(Integer previousReading) { this.previousReading = previousReading; }
    public Integer getCurrentReading() { return currentReading; }
    public void setCurrentReading(Integer currentReading) { this.currentReading = currentReading; }
    public Integer getUsageUnits() { return usageUnits; }
    public void setUsageUnits(Integer usageUnits) { this.usageUnits = usageUnits; }
    public LocalDate getReadingDate() { return readingDate; }
    public void setReadingDate(LocalDate readingDate) { this.readingDate = readingDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
}

