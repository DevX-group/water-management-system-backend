package com.backend.water_management_system.alerts.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.alerts.entity.Alert;
import com.backend.water_management_system.alerts.repository.AlertRepository;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    public List<Alert> getActiveAlerts(String severity) {        // If severity is provided and not "all", filter by severity
        if (severity != null && !severity.equalsIgnoreCase("all")) {
            return alertRepository.findBySeverityAndDismissedFalse(severity.toLowerCase());
        }
        return alertRepository.findByDismissedFalseOrderByTimeDesc();
    }

    public List<Alert> getActiveAlertsForCustomer(String severity, String customerId) {
        if (severity != null && !severity.equalsIgnoreCase("all")) {
            return alertRepository.findForCustomerAndSeverity(customerId, severity.toLowerCase());
        }
        return alertRepository.findForCustomerOrderByTimeDesc(customerId);
    }

    public Map<String, Long> getSeverityCounts() {        // Get all active alerts and group by severity to count 
        List<Alert> activeAlerts = alertRepository.findByDismissedFalseOrderByTimeDesc();
        return activeAlerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));
    }

    public Map<String, Long> getSeverityCountsForCustomer(String customerId) {
        List<Alert> activeAlerts = alertRepository.findForCustomerOrderByTimeDesc(customerId);
        return activeAlerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));
    }

    public void dismissAlert(Long id) {         // Find the alert by ID, set dismissed to true
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setDismissed(true);
            alertRepository.save(alert);
        });
    }

    public void createAlert(String severity, String title, String description, String usage, String customerId) {       // Create a new alert
        Alert alert = Alert.builder()
                .severity(severity)
                .title(title)
                .description(description)
                .usage(usage)
                .customerId(customerId)
                .time(java.time.LocalDateTime.now())
                .dismissed(false)
                .build();
        alertRepository.save(alert);
    }
}