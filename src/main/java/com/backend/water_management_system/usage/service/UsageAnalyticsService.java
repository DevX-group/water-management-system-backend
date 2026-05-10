package com.backend.water_management_system.usage.service;
import com.backend.water_management_system.usage.dto.UsageAnalyticsResponse;
import com.backend.water_management_system.usage.dto.UsageAnalyticsResponse.CategoryDataPoint;
import com.backend.water_management_system.usage.dto.UsageAnalyticsResponse.MonthlyDataPoint;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.meter_reading.repository.MeterReadingRepository;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Aggregates MeterReading data into the shape expected by the
 * Usage Trends analytics page on the frontend.
 *
 * Two public methods are provided:
 *  - getAnalytics(year)                   → system-wide (admin view)
 *  - getAnalytics(subscriptionNumber, year) → single-customer view
 */
@Service
public class UsageAnalyticsService {
    // Monthly limit shown as a reference line on the Mix chart
    private static final int MONTHLY_LIMIT = 150;
    // Pie-chart colour palette (must match frontend categoryData colours)
    private static final String COLOR_DOMESTIC    = "#0ea5e9";
    private static final String COLOR_GARDEN      = "#38bdf8";
    private static final String COLOR_MAINTENANCE = "#bae6fd";
    private final MeterReadingRepository meterReadingRepository;
    public UsageAnalyticsService(MeterReadingRepository meterReadingRepository) {
        this.meterReadingRepository = meterReadingRepository;
    }
    // ── Public API ────────────────────────────────────────────────────────────
    /** System-wide analytics for the given calendar year. */
    public UsageAnalyticsResponse getAnalytics(int year) {
        List<MeterReading> readings = meterReadingRepository.findAllByYear(year);
        return buildResponse(readings);
    }
    /** Per-customer analytics for the given calendar year. */
    public UsageAnalyticsResponse getAnalytics(String subscriptionNumber, int year) {
        List<MeterReading> readings =
                meterReadingRepository.findByCustomerAndYear(subscriptionNumber, year);
        return buildResponse(readings);
    }
    // ── Private helpers ───────────────────────────────────────────────────────
    private UsageAnalyticsResponse buildResponse(List<MeterReading> readings) {
        UsageAnalyticsResponse response = new UsageAnalyticsResponse();
        // ── 1. Aggregate usage per calendar month (1-12) ─────────────────────
        // Sum usageUnits for every reading that falls in each month.
        Map<Integer, Integer> usageByMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            usageByMonth.put(m, 0);
        }
        for (MeterReading r : readings) {
            if (r.getReadingDate() == null || r.getUsageUnits() == null) continue;
            int month = r.getReadingDate().getMonthValue();
            usageByMonth.merge(month, r.getUsageUnits(), Integer::sum);
        }
        // ── 2. Build MonthlyDataPoint list ────────────────────────────────────
        List<MonthlyDataPoint> monthlyData = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : usageByMonth.entrySet()) {
            String monthName = Month.of(entry.getKey())
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // "Jan", "Feb" …
            monthlyData.add(new MonthlyDataPoint(monthName, entry.getValue(), MONTHLY_LIMIT));
        }
        response.monthlyData = monthlyData;
        // ── 3. Calculate summary statistics ──────────────────────────────────
        List<Integer> monthlyValues = new ArrayList<>(usageByMonth.values());
        int total = monthlyValues.stream().mapToInt(Integer::intValue).sum();
        int peak  = monthlyValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int min   = monthlyValues.stream().mapToInt(Integer::intValue).min().orElse(0);
        int avg   = monthlyValues.isEmpty() ? 0 : total / monthlyValues.size();
        response.totalUsage   = total;
        response.peakUsage    = peak;
        response.minimumUsage = min;
        response.averageUsage = avg;
        // ── 4. Build CategoryDataPoint list (Pie chart) ───────────────────────
        // Category split is derived from connectionType on each reading's customer.
        //   "metered"     → Domestic  (sky-500)
        //   "garden"      → Garden    (sky-400)   ← future connection type
        //   anything else → Maintenance            (sky-200)
        //
        // If no connectionType data is present the split falls back to the
        // same proportions used in the frontend mock (65 / 20 / 15).
        response.categoryData = buildCategoryData(readings, total);
        return response;
    }
    private List<CategoryDataPoint> buildCategoryData(List<MeterReading> readings, int total) {
        // Use real connection-type split when readings are available
        int domesticUnits    = 0;
        int gardenUnits      = 0;
        int maintenanceUnits = 0;
        for (MeterReading r : readings) {
            if (r.getUsageUnits() == null) continue;
            String type = r.getCustomer() != null
                    ? r.getCustomer().getConnectionType()
                    : null;
            if (type == null) {
                domesticUnits += r.getUsageUnits();
            } else {
                switch (type.toLowerCase()) {
                    case "garden"      -> gardenUnits      += r.getUsageUnits();
                    case "maintenance" -> maintenanceUnits += r.getUsageUnits();
                    default            -> domesticUnits    += r.getUsageUnits(); // "metered" etc.
                }
            }
        }
        // Fall back to frontend mock proportions when there is no data yet
        if (total == 0) {
            return List.of(
                    new CategoryDataPoint("Domestic",    65, COLOR_DOMESTIC),
                    new CategoryDataPoint("Garden",      20, COLOR_GARDEN),
                    new CategoryDataPoint("Maintenance", 15, COLOR_MAINTENANCE)
            );
        }
        double domesticPct    = round((domesticUnits    * 100.0) / total);
        double gardenPct      = round((gardenUnits      * 100.0) / total);
        double maintenancePct = round((maintenanceUnits * 100.0) / total);
        return List.of(
                new CategoryDataPoint("Domestic",    domesticPct,    COLOR_DOMESTIC),
                new CategoryDataPoint("Garden",      gardenPct,      COLOR_GARDEN),
                new CategoryDataPoint("Maintenance", maintenancePct, COLOR_MAINTENANCE)
        );
    }
    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
