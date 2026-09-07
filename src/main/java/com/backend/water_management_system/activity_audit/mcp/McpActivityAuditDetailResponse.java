package com.backend.water_management_system.activity_audit.mcp;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.user.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** MCP-specific audit detail that omits legitimate null system-actor fields. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpActivityAuditDetailResponse(
        UUID id,
        Instant occurredAt,
        UUID actorUserId,
        String actorDisplayName,
        Role actorRole,
        AuditAction action,
        AuditEntityType entityType,
        String entityId,
        String summary,
        Map<String, String> changedFields,
        AuditSource source) {

    static McpActivityAuditDetailResponse from(ActivityAuditDetailResponse response) {
        return new McpActivityAuditDetailResponse(
                response.id(),
                response.occurredAt(),
                response.actorUserId(),
                response.actorDisplayName(),
                response.actorRole(),
                response.action(),
                response.entityType(),
                response.entityId(),
                response.summary(),
                response.changedFields(),
                response.source());
    }
}
