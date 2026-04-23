package com.backend.water_management_system.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.AlertResponse;
import com.backend.water_management_system.entity.Alert;
import com.backend.water_management_system.repository.AlertRepository;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<AlertResponse> getActiveAlerts(String subNum) {
        return alertRepository.findBySubscriptionNumberAndDismissedFalseOrderByCreatedAtDesc(subNum)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> list));
    }

    public void dismissAlert(Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setDismissed(true);
            alertRepository.save(alert);
        });
    }

    private AlertResponse convertToDto(Alert alert) {
        AlertResponse dto = new AlertResponse();
        dto.setId(alert.getId());
        dto.setSeverity(alert.getSeverity());
        dto.setTitle(alert.getTitle());
        dto.setDescription(alert.getDescription());
        dto.setUsageAmount(alert.getUsageAmount());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setSubscriptionNumber(alert.getSubscriptionNumber());
        return dto;
    }
}