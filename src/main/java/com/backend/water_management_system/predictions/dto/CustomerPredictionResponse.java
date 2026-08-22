package com.backend.water_management_system.predictions.dto;

public class CustomerPredictionResponse {

    private final String month;
    private final Double usage;
    private final Double revenue;
    private final Double predictedUsage;
    private final Double predictedRevenue;

    public CustomerPredictionResponse(
            String month,
            Double usage,
            Double revenue,
            Double predictedUsage,
            Double predictedRevenue
    ) {
        this.month = month;
        this.usage = usage;
        this.revenue = revenue;
        this.predictedUsage = predictedUsage;
        this.predictedRevenue = predictedRevenue;
    }

    public CustomerPredictionResponse(
            String month,
            Double usage,
            Double predictedUsage
    ) {
        this(
                month,
                usage,
                null,
                predictedUsage,
                null
        );
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

    public Double getPredictedUsage() {
        return predictedUsage;
    }

    public Double getPredictedRevenue() {
        return predictedRevenue;
    }
}