package com.backend.water_management_system.notification.dto;

import java.time.LocalDateTime;

import com.backend.water_management_system.notification.enums.NotificationType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String subscriptionNumber;
    private NotificationType notificationType;
    private String title;
    private String message;
    private boolean readStatus;
    private LocalDateTime createdAt;
}
