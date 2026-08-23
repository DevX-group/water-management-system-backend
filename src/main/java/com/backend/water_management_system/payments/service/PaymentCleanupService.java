package com.backend.water_management_system.payments.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import java.util.Map;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCleanupService {

    private final PaymentRepository paymentRepository;
    private final ActivityAuditService activityAuditService;

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void expireOldPendingPayments() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<Payment> oldPendingPayments =
            paymentRepository.findByStatusAndPaymentMethodAndCreatedAtBefore(
                PaymentStatus.PENDING,
                PaymentMethod.ONLINE,
                cutoff
            );

        List<Payment> changedPayments = oldPendingPayments.stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
            .toList();
        changedPayments.forEach(payment -> payment.setStatus(PaymentStatus.EXPIRED));

        paymentRepository.saveAll(changedPayments);
        changedPayments.forEach(payment -> activityAuditService.recordSystem(
                AuditAction.PAYMENT_STATUS_CHANGED, AuditEntityType.PAYMENT,
                payment.getPaymentId(), Map.of("status", "PENDING -> EXPIRED")));
    }
}
