package com.backend.water_management_system.predictions.dto;

public class AreaPredictionResponse {

    private String month;

    private Double area1Usage;
    private Double area1Revenue;
    private Double predictedArea1Usage;
    private Double predictedArea1Revenue;

    private Double area2Usage;
    private Double area2Revenue;
    private Double predictedArea2Usage;
    private Double predictedArea2Revenue;

    private Double area3Usage;
    private Double area3Revenue;
    private Double predictedArea3Usage;
    private Double predictedArea3Revenue;

    public AreaPredictionResponse(String month) {
        this.month = month;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Double getArea1Usage() {
        return area1Usage;
    }

    public void setArea1Usage(Double area1Usage) {
        this.area1Usage = area1Usage;
    }

    public Double getArea1Revenue() {
        return area1Revenue;
    }

    public void setArea1Revenue(Double area1Revenue) {
        this.area1Revenue = area1Revenue;
    }

    public Double getPredictedArea1Usage() {
        return predictedArea1Usage;
    }

    public void setPredictedArea1Usage(
            Double predictedArea1Usage
    ) {
        this.predictedArea1Usage =
                predictedArea1Usage;
    }

    public Double getPredictedArea1Revenue() {
        return predictedArea1Revenue;
    }

    public void setPredictedArea1Revenue(
            Double predictedArea1Revenue
    ) {
        this.predictedArea1Revenue =
                predictedArea1Revenue;
    }

    public Double getArea2Usage() {
        return area2Usage;
    }

    public void setArea2Usage(Double area2Usage) {
        this.area2Usage = area2Usage;
    }

    public Double getArea2Revenue() {
        return area2Revenue;
    }

    public void setArea2Revenue(Double area2Revenue) {
        this.area2Revenue = area2Revenue;
    }

    public Double getPredictedArea2Usage() {
        return predictedArea2Usage;
    }

    public void setPredictedArea2Usage(
            Double predictedArea2Usage
    ) {
        this.predictedArea2Usage =
                predictedArea2Usage;
    }

    public Double getPredictedArea2Revenue() {
        return predictedArea2Revenue;
    }

    public void setPredictedArea2Revenue(
            Double predictedArea2Revenue
    ) {
        this.predictedArea2Revenue =
                predictedArea2Revenue;
    }

    public Double getArea3Usage() {
        return area3Usage;
    }

    public void setArea3Usage(Double area3Usage) {
        this.area3Usage = area3Usage;
    }

    public Double getArea3Revenue() {
        return area3Revenue;
    }

    public void setArea3Revenue(Double area3Revenue) {
        this.area3Revenue = area3Revenue;
    }

    public Double getPredictedArea3Usage() {
        return predictedArea3Usage;
    }

    public void setPredictedArea3Usage(
            Double predictedArea3Usage
    ) {
        this.predictedArea3Usage =
                predictedArea3Usage;
    }

    public Double getPredictedArea3Revenue() {
        return predictedArea3Revenue;
    }

    public void setPredictedArea3Revenue(
            Double predictedArea3Revenue
    ) {
        this.predictedArea3Revenue =
                predictedArea3Revenue;
    }
}