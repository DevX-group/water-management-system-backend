package com.backend.water_management_system.activity_audit.dto;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.user.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record ActivityAuditListResponse(
        UUID id,
        Instant occurredAt,
        UUID actorUserId,
        String actorDisplayName,
        Role actorRole,
        AuditAction action,
        AuditEntityType entityType,
        String entityId,
        String summary,
        AuditSource source) {
}
