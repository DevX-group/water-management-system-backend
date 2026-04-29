package com.backend.water_management_system.dto;

public class CustomerReportDTO {

    private String month;
    private Double totalUsage;
    private Double totalAmount;

    public CustomerReportDTO(String month, Double totalUsage, Double totalAmount) {
        this.month = month;
        this.totalUsage = totalUsage;
        this.totalAmount = totalAmount;
    }

    public String getMonth() {
        return month;
    }

    public Double getTotalUsage() {
        return totalUsage;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }
}