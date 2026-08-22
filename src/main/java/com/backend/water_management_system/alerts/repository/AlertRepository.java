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

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Alert a WHERE (a.customerId = :customerId OR a.customerId IS NULL) AND a.dismissed = false ORDER BY a.time DESC")
    List<Alert> findForCustomerOrderByTimeDesc(@org.springframework.data.repository.query.Param("customerId") String customerId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Alert a WHERE (a.customerId = :customerId OR a.customerId IS NULL) AND a.severity = :severity AND a.dismissed = false ORDER BY a.time DESC")
    List<Alert> findForCustomerAndSeverity(@org.springframework.data.repository.query.Param("customerId") String customerId, @org.springframework.data.repository.query.Param("severity") String severity);
}