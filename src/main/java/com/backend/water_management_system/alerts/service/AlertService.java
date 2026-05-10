package com.backend.water_management_system.alerts.service;

import com.backend.water_management_system.alerts.entity.Alert;
import com.backend.water_management_system.alerts.repository.AlertRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    public List<Alert> getActiveAlerts(String severity) {
        if (severity != null && !severity.equalsIgnoreCase("all")) {
            return alertRepository.findBySeverityAndDismissedFalse(severity.toLowerCase());
        }
        return alertRepository.findByDismissedFalseOrderByTimeDesc();
    }

    public Map<String, Long> getSeverityCounts() {
        List<Alert> activeAlerts = alertRepository.findByDismissedFalseOrderByTimeDesc();
        return activeAlerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));
    }

    public void dismissAlert(Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setDismissed(true);
            alertRepository.save(alert);
        });
    }

    public void createAlert(String severity, String title, String description, String usage) {
        Alert alert = Alert.builder()
                .severity(severity)
                .title(title)
                .description(description)
                .usage(usage)
                .time(java.time.LocalDateTime.now())
                .dismissed(false)
                .build();
        alertRepository.save(alert);
    }
}