package com.backend.water_management_system.activity_audit.repository;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ActivityAuditLogRepository extends JpaRepository<ActivityAuditLog, UUID>,
        JpaSpecificationExecutor<ActivityAuditLog> {
}
