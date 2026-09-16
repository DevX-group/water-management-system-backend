package com.backend.water_management_system.activity_audit.mcp;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.user.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * MCP-specific audit list item.
 *
 * <p>System-generated audit events have no user actor. Omitting those null actor
 * properties keeps the MCP JSON concise without changing the existing REST
 * response contract.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpActivityAuditListResponse(
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

    static McpActivityAuditListResponse from(ActivityAuditListResponse response) {
        return new McpActivityAuditListResponse(
                response.id(),
                response.occurredAt(),
                response.actorUserId(),
                response.actorDisplayName(),
                response.actorRole(),
                response.action(),
                response.entityType(),
                response.entityId(),
                response.summary(),
                response.source());
    }
}
