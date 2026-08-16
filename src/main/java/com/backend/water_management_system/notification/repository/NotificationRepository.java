package com.backend.water_management_system.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get all notifications for a customer (for the bell icon drop-down)
    List<Notification> findTop20BySubscriptionNumberOrderByCreatedAtDesc(String subscriptionNumber);

    // Get only unread notifications (the count in the bell icon)
    List<Notification> findBySubscriptionNumberAndReadStatusFalseOrderByCreatedAtDesc(String subscriptionNumber);

    // To delete by ID (used in mark as read)
    Optional<Notification> findByIdAndSubscriptionNumber(Long id, String subscriptionNumber);

}
