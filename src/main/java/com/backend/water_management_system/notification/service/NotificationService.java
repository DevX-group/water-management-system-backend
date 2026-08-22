package com.backend.water_management_system.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.notification.dto.NotificationRequest;
import com.backend.water_management_system.notification.dto.NotificationResponse;
import com.backend.water_management_system.notification.entity.Notification;
import com.backend.water_management_system.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Creates, saves, and sends a notification to a customer.
    public NotificationResponse sendNotification(NotificationRequest request) {

        // Create notification entity
        Notification notification = Notification.builder()
                .subscriptionNumber(request.getSubscriptionNumber())
                .notificationType(request.getNotificationType())
                .title(request.getTitle())
                .message(request.getMessage())
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Save notification to database
        Notification savedNotification = notificationRepository.save(notification);

        // Convert entity to response DTO
        NotificationResponse response = mapToResponse(savedNotification);

        // Send notification through WebSocket
        messagingTemplate.convertAndSend(
                "/topic/customer/" + request.getSubscriptionNumber(),
                response);

        return response;
    }

    // Get all notifications for a customer.
    public List<NotificationResponse> getCustomerNotifications(String subscriptionNumber) {

        return notificationRepository
                .findTop20BySubscriptionNumberOrderByCreatedAtDesc(subscriptionNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get unread notifications for a customer.
    public List<NotificationResponse> getUnreadNotifications(String subscriptionNumber) {

        return notificationRepository
                .findBySubscriptionNumberAndReadStatusFalseOrderByCreatedAtDesc(subscriptionNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Mark a notification as read.
    public void markAsRead(Long notificationId, String subscriptionNumber) {

        Notification notification = notificationRepository
                .findByIdAndSubscriptionNumber(notificationId, subscriptionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setReadStatus(true);

        notificationRepository.save(notification);
    }

    // Mark all notifications as read for a customer.
    public void markAllAsRead(String subscriptionNumber) {

        List<Notification> notifications = notificationRepository
                .findBySubscriptionNumberAndReadStatusFalseOrderByCreatedAtDesc(subscriptionNumber);

        notifications.forEach(notification -> notification.setReadStatus(true));

        notificationRepository.saveAll(notifications);
    }

    // Convert Notification entity to NotificationResponse DTO.
    private NotificationResponse mapToResponse(Notification notification) {

        return NotificationResponse.builder()
                .id(notification.getId())
                .subscriptionNumber(notification.getSubscriptionNumber())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .readStatus(notification.isReadStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }

}
