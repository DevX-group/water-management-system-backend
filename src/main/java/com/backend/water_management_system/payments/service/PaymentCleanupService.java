package com.backend.water_management_system.payments.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCleanupService {

    private final PaymentRepository paymentRepository;

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

        for (Payment payment : oldPendingPayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
        }

        paymentRepository.saveAll(oldPendingPayments);
    }
}