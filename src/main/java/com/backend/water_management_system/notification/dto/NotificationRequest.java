package com.backend.water_management_system.notification.dto;

import com.backend.water_management_system.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private String subscriptionNumber;
    private NotificationType notificationType;
    private String title;
    private String message;
}