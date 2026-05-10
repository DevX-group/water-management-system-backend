package com.backend.water_management_system.usage.dto;
import java.util.List;
/**
 * Full response payload for the Usage Trends analytics page.
 * Maps directly to the chart data and stat cards shown in the frontend.
 */
public class UsageAnalyticsResponse {
    // ── Summary stat cards ──────────────────────────────────────────────────
    public int averageUsage;   // Average monthly usage (units)
    public int peakUsage;      // Highest single-month usage
    public int minimumUsage;   // Lowest single-month usage
    public int totalUsage;     // Sum of all usage across the year
    // ── Bar / Mix chart data  (one entry per month) ─────────────────────────
    public List<MonthlyDataPoint> monthlyData;
    // ── Pie chart data  (usage by category) ─────────────────────────────────
    public List<CategoryDataPoint> categoryData;
    // ────────────────────────────────────────────────────────────────────────
    /** One bar / area / line data-point on the month-by-month chart. */
    public static class MonthlyDataPoint {
        public String name;   // "Jan", "Feb", …
        public int usage;     // Actual usage that month
        public int limit;     // Monthly quota/limit
        public MonthlyDataPoint(String name, int usage, int limit) {
            this.name  = name;
            this.usage = usage;
            this.limit = limit;
        }
    }
    /** One slice of the pie chart (usage by connection category). */
    public static class CategoryDataPoint {
        public String name;   // "Domestic", "Garden", "Maintenance"
        public double value;  // Percentage share
        public String color;  // Hex colour used by Recharts
        public CategoryDataPoint(String name, double value, String color) {
            this.name  = name;
            this.value = value;
            this.color = color;
        }
    }
}