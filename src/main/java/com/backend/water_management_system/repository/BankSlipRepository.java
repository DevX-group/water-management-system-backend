package com.backend.water_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.entity.BankSlip;
import com.backend.water_management_system.entity.SlipStatus;

public interface BankSlipRepository extends JpaRepository<BankSlip, Long> {
    List<BankSlip> findByStatus(SlipStatus status);

    Boolean existsByBankReference(String bankReference);

    List<BankSlip> findBySubscriptionNumberOrderByUploadedAtDesc(String subscriptionNumber);
}

