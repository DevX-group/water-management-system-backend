package com.backend.water_management_system.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AlertResponse {
    private Long id;
    private String severity;
    private String title;
    private String description;
    private String usageAmount;
    private LocalDateTime createdAt;
    private String subscriptionNumber;
}