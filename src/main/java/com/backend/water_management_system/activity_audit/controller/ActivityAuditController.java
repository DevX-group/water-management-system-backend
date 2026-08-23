package com.backend.water_management_system.activity_audit.controller;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.service.ActivityAuditQueryService;
import com.backend.water_management_system.common.dto.PaginationResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/activity-logs")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
public class ActivityAuditController {

    private final ActivityAuditQueryService queryService;

    public ActivityAuditController(ActivityAuditQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PaginationResponse<ActivityAuditListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditSource source) {
        return queryService.findAll(page, size, sortBy, sortDirection,
                from, to, action, entityType, actorUserId, actor, source);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public PaginationResponse<ActivityAuditListResponse> entityHistory(
            @PathVariable AuditEntityType entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.findEntityHistory(entityType, entityId, page, size);
    }

    @GetMapping("/{id}")
    public ActivityAuditDetailResponse detail(@PathVariable UUID id) {
        return queryService.findById(id);
    }
}
