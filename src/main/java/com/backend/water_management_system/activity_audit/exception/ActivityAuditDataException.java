package com.backend.water_management_system.activity_audit.exception;

public class ActivityAuditDataException extends RuntimeException {
    public ActivityAuditDataException() {
        super("Stored activity audit details could not be read safely.");
    }
}
