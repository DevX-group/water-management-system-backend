package com.backend.water_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.entity.Alert;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findBySubscriptionNumberAndDismissedFalseOrderByCreatedAtDesc(String subNum);
}