package com.backend.water_management_system.activity_audit.exception;

import java.util.UUID;

public class ActivityAuditLogNotFoundException extends RuntimeException {
    public ActivityAuditLogNotFoundException(UUID id) {
        super("Activity audit log not found: " + id);
    }
}
