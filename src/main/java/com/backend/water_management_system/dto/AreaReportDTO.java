package com.backend.water_management_system.dto;

public class AreaReportDTO {

    private String month;

    private Double area1Usage = 0.0;
    private Double area1Revenue = 0.0;

    private Double area2Usage = 0.0;
    private Double area2Revenue = 0.0;

    private Double area3Usage = 0.0;
    private Double area3Revenue = 0.0;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Double getArea1Usage() { return area1Usage; }
    public void setArea1Usage(Double area1Usage) { this.area1Usage = area1Usage; }

    public Double getArea1Revenue() { return area1Revenue; }
    public void setArea1Revenue(Double area1Revenue) { this.area1Revenue = area1Revenue; }

    public Double getArea2Usage() { return area2Usage; }
    public void setArea2Usage(Double area2Usage) { this.area2Usage = area2Usage; }

    public Double getArea2Revenue() { return area2Revenue; }
    public void setArea2Revenue(Double area2Revenue) { this.area2Revenue = area2Revenue; }

    public Double getArea3Usage() { return area3Usage; }
    public void setArea3Usage(Double area3Usage) { this.area3Usage = area3Usage; }

    public Double getArea3Revenue() { return area3Revenue; }
    public void setArea3Revenue(Double area3Revenue) { this.area3Revenue = area3Revenue; }
}