package com.backend.water_management_system.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.customer.service.CustomerAccessService;
import com.backend.water_management_system.notification.dto.NotificationResponse;
import com.backend.water_management_system.notification.service.NotificationService;
import com.backend.water_management_system.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customer/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class NotificationController {
    private final NotificationService notificationService;
    private final CustomerAccessService customerAccessService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {

        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);

        return ResponseEntity.ok(
                notificationService.getCustomerNotifications(subscriptionNumber));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {

        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(subscriptionNumber));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);

        notificationService.markAsRead(id, subscriptionNumber);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);

        notificationService.markAllAsRead(subscriptionNumber);

        return ResponseEntity.ok().build();
    }
}
