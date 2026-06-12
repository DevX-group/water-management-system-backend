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

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin
public class UsageAnalyticsController {
    private final UsageAnalyticsService usageAnalyticsService;
    public UsageAnalyticsController(UsageAnalyticsService usageAnalyticsService) {
        this.usageAnalyticsService = usageAnalyticsService;
    }
   
    @GetMapping("/usage")      // Get system-wide usage analytics, optionally filtered by year
    public ResponseEntity<UsageAnalyticsResponse> getSystemUsage(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(usageAnalyticsService.getAnalytics(targetYear));
    }
   
    @GetMapping("/usage/{subscriptionNumber}")      // Get usage analytics for a specific customer by subscription number
    public ResponseEntity<UsageAnalyticsResponse> getCustomerUsage(
            @PathVariable String subscriptionNumber,
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                usageAnalyticsService.getAnalytics(subscriptionNumber, targetYear));
    }
}
