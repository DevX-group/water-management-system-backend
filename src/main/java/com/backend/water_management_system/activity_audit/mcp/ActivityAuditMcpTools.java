package com.backend.water_management_system.activity_audit.mcp;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.service.ActivityAuditQueryService;
import com.backend.water_management_system.common.dto.PaginationResponse;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class ActivityAuditMcpTools {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_FIELD = "occurredAt";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private final ActivityAuditQueryService queryService;

    public ActivityAuditMcpTools(ActivityAuditQueryService queryService) {
        this.queryService = queryService;
    }

    @McpTool(
            name = "search_activity_logs",
            title = "Search activity audit logs",
            description = "Search read-only administrative activity audit logs. "
                    + "All filters are optional. Timestamps must use ISO-8601 UTC format, for example "
                    + "2026-08-23T10:00:00Z. Enum values must match the documented uppercase values.",
            generateOutputSchema = false,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public PaginationResponse<McpActivityAuditListResponse> searchActivityLogs(
            @McpToolParam(description = "Zero-based page number. Defaults to 0.", required = false)
            Integer page,
            @McpToolParam(description = "Results per page from 1 to 100. Defaults to 20.", required = false)
            Integer size,
            @McpToolParam(description = "Sort field: occurredAt, action, entityType, or source. Defaults to occurredAt.", required = false)
            String sortBy,
            @McpToolParam(description = "Sort direction: asc or desc. Defaults to desc.", required = false)
            String sortDirection,
            @McpToolParam(description = "Optional inclusive start timestamp in ISO-8601 format.", required = false)
            String from,
            @McpToolParam(description = "Optional inclusive end timestamp in ISO-8601 format.", required = false)
            String to,
            @McpToolParam(description = "Optional audit action, for example PAYMENT_CREATED.", required = false)
            String action,
            @McpToolParam(description = "Optional entity type: USER, PAYMENT, or METER_READING.", required = false)
            String entityType,
            @McpToolParam(description = "Optional actor user UUID.", required = false)
            String actorUserId,
            @McpToolParam(description = "Optional case-insensitive actor display-name search.", required = false)
            String actor,
            @McpToolParam(description = "Optional source: WEB, SYSTEM, or MCP.", required = false)
            String source) {
        return mcpPage(queryService.findAll(
                defaultInteger(page, DEFAULT_PAGE),
                defaultInteger(size, DEFAULT_PAGE_SIZE),
                defaultString(sortBy, DEFAULT_SORT_FIELD),
                defaultString(sortDirection, DEFAULT_SORT_DIRECTION),
                parseInstant(from, "from"),
                parseInstant(to, "to"),
                parseEnum(action, AuditAction.class, "action"),
                parseEnum(entityType, AuditEntityType.class, "entityType"),
                parseUuid(actorUserId, "actorUserId"),
                blankToNull(actor),
                parseEnum(source, AuditSource.class, "source")));
    }

    @McpTool(
            name = "get_activity_log",
            title = "Get an activity audit log",
            description = "Get the read-only details of one administrative activity audit event by its UUID.",
            generateOutputSchema = false,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public McpActivityAuditDetailResponse getActivityLog(
            @McpToolParam(description = "Activity audit log UUID.", required = true) String id) {
        return McpActivityAuditDetailResponse.from(queryService.findById(requireUuid(id, "id")));
    }

    @McpTool(
            name = "get_entity_activity_history",
            title = "Get entity activity history",
            description = "Get a paginated, read-only activity history for one USER, PAYMENT, or METER_READING entity.",
            generateOutputSchema = false,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public PaginationResponse<McpActivityAuditListResponse> getEntityActivityHistory(
            @McpToolParam(description = "Entity type: USER, PAYMENT, or METER_READING.", required = true)
            String entityType,
            @McpToolParam(description = "Entity identifier in the format used by the selected entity type.", required = true)
            String entityId,
            @McpToolParam(description = "Zero-based page number. Defaults to 0.", required = false)
            Integer page,
            @McpToolParam(description = "Results per page from 1 to 100. Defaults to 20.", required = false)
            Integer size) {
        String normalizedEntityId = blankToNull(entityId);
        if (normalizedEntityId == null) {
            throw new IllegalArgumentException("entityId is required.");
        }
        return mcpPage(queryService.findEntityHistory(
                requireEnum(entityType, AuditEntityType.class, "entityType"),
                normalizedEntityId,
                defaultInteger(page, DEFAULT_PAGE),
                defaultInteger(size, DEFAULT_PAGE_SIZE)));
    }

    private PaginationResponse<McpActivityAuditListResponse> mcpPage(
            PaginationResponse<ActivityAuditListResponse> page) {
        return PaginationResponse.<McpActivityAuditListResponse>builder()
                .content(page.getContent().stream()
                        .map(McpActivityAuditListResponse::from)
                        .toList())
                .currentPage(page.getCurrentPage())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getPageSize())
                .last(page.isLast())
                .build();
    }

    private int defaultInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultString(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private Instant parseInstant(String value, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Instant.parse(normalized);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 UTC timestamp.");
        }
    }

    private UUID parseUuid(String value, String field) {
        String normalized = blankToNull(value);
        return normalized == null ? null : uuid(normalized, field);
    }

    private UUID requireUuid(String value, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return uuid(normalized, field);
    }

    private UUID uuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a valid UUID.");
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String field) {
        String normalized = blankToNull(value);
        return normalized == null ? null : enumValue(normalized, enumType, field);
    }

    private <E extends Enum<E>> E requireEnum(String value, Class<E> enumType, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return enumValue(normalized, enumType, field);
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> enumType, String field) {
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " has an unsupported value.");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
