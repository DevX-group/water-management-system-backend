package com.backend.water_management_system.payments.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.water_management_system.payments.entity.BankSlip;
import com.backend.water_management_system.payments.enums.SlipStatus;

public interface BankSlipRepository extends JpaRepository<BankSlip, Long> {
    List<BankSlip> findByStatusOrderByUploadedAtDesc(SlipStatus status);

    Boolean existsByBankReference(String bankReference);

    Page<BankSlip> findBySubscriptionNumberOrderByUploadedAtDesc(String subscriptionNumber, Pageable pageable);

    @Query("""
                SELECT s FROM BankSlip s
                JOIN Customer c ON s.subscriptionNumber = c.subscriptionNumber
                WHERE s.status = 'PENDING'
                AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(s.subscriptionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(c.accountHolderName) LIKE LOWER(CONCAT('%', :search, '%'))
                )
            """)
    Page<BankSlip> searchPendingSlips( @Param("search") String search, Pageable pageable);
}
