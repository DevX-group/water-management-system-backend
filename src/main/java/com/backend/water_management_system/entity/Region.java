package com.backend.water_management_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="regions")
public class Region {
    @Id
    private String regionCode; // e.g. "R001"

    private String regionName;

    // base charge (fixed)
    private BigDecimal baseRate;

    // tier rates
    private BigDecimal unitRateTier1;
    private BigDecimal unitRateTier2;
    private BigDecimal unitRateTier3;

    // tax percentage (e.g., 0.08 = 8%)
    private BigDecimal taxRate;

    // getters/setters
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public BigDecimal getUnitRateTier1() { return unitRateTier1; }
    public void setUnitRateTier1(BigDecimal unitRateTier1) { this.unitRateTier1 = unitRateTier1; }
    public BigDecimal getUnitRateTier2() { return unitRateTier2; }
    public void setUnitRateTier2(BigDecimal unitRateTier2) { this.unitRateTier2 = unitRateTier2; }
    public BigDecimal getUnitRateTier3() { return unitRateTier3; }
    public void setUnitRateTier3(BigDecimal unitRateTier3) { this.unitRateTier3 = unitRateTier3; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public Region() {}
    
    public Region(String regionCode, String regionName) {
        this.regionCode = regionCode;
        this.regionName = regionName;
    }
}

