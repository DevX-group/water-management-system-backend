package com.backend.water_management_system.usage.controller;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.usage.dto.UsageAnalyticsResponse;
import com.backend.water_management_system.usage.service.UsageAnalyticsService;
/**
 * REST endpoints for the Usage Trends analytics page.
 *
 * GET /api/analytics/usage?year=2026
 *      → System-wide monthly usage for the given year (admin view).
 *
 * GET /api/analytics/usage/{subscriptionNumber}?year=2026
 *      → Usage analytics scoped to a single customer.
 *
 * Both endpoints default to the current calendar year when 'year' is omitted.
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin
public class UsageAnalyticsController {
    private final UsageAnalyticsService usageAnalyticsService;
    public UsageAnalyticsController(UsageAnalyticsService usageAnalyticsService) {
        this.usageAnalyticsService = usageAnalyticsService;
    }
    /**
     * System-wide usage analytics.
     *
     * Example:  GET /api/analytics/usage
     *           GET /api/analytics/usage?year=2025
     */
    @GetMapping("/usage")
    public ResponseEntity<UsageAnalyticsResponse> getSystemUsage(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(usageAnalyticsService.getAnalytics(targetYear));
    }
    /**
     * Per-customer usage analytics.
     *
     * Example:  GET /api/analytics/usage/SUB-001
     *           GET /api/analytics/usage/SUB-001?year=2025
     */
    @GetMapping("/usage/{subscriptionNumber}")
    public ResponseEntity<UsageAnalyticsResponse> getCustomerUsage(
            @PathVariable String subscriptionNumber,
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                usageAnalyticsService.getAnalytics(subscriptionNumber, targetYear));
    }
}
