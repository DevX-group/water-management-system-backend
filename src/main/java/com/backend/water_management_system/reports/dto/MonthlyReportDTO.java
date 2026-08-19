package com.backend.water_management_system.reports.dto;

public class MonthlyReportDTO {

    private String month;
    private Double usage;
    private Double revenue;

    public MonthlyReportDTO(String month, Double usage, Double revenue) {
        this.month = month;
        this.usage = usage;
        this.revenue = revenue;
    }

    public String getMonth() {
        return month;
    }

    public Double getUsage() {
        return usage;
    }

    public Double getRevenue() {
        return revenue;
    }
}
