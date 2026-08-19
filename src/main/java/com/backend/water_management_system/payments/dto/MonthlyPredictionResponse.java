package com.backend.water_management_system.dto;

public class MonthlyPredictionResponse {

    private String month;
    private Double usage;
    private Double predictedUsage;

    public MonthlyPredictionResponse(String month, Double usage, Double predictedUsage) {
        this.month = month;
        this.usage = usage;
        this.predictedUsage = predictedUsage;
    }

    public String getMonth() {
        return month;
    }

    public Double getUsage() {
        return usage;
    }

    public Double getPredictedUsage() {
        return predictedUsage;
    }
}