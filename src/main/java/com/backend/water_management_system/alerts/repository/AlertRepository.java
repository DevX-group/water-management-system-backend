package com.backend.water_management_system.alerts.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.alerts.entity.Alert;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> //Primary Key (@Id) of the Alert is of type Long 
{
    List<Alert> findByDismissedFalseOrderByTimeDesc();
    List<Alert> findBySeverityAndDismissedFalse(String severity);
}