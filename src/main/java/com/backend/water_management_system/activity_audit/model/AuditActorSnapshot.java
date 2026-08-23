package com.backend.water_management_system.activity_audit.model;

import com.backend.water_management_system.user.enums.Role;

import java.util.UUID;

public record AuditActorSnapshot(UUID userId, String displayName, Role role) {
}
