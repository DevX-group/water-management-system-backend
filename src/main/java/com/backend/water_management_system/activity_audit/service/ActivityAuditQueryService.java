package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.exception.ActivityAuditLogNotFoundException;
import com.backend.water_management_system.activity_audit.mapper.ActivityAuditMapper;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditSpecifications;
import com.backend.water_management_system.common.dto.PaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActivityAuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("occurredAt", "action", "entityType", "source");

    private final ActivityAuditLogRepository repository;
    private final ActivityAuditMapper mapper;

    public ActivityAuditQueryService(ActivityAuditLogRepository repository, ActivityAuditMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PaginationResponse<ActivityAuditListResponse> findAll(
            int page, int size, String sortBy, String sortDirection,
            Instant from, Instant to, AuditAction action, AuditEntityType entityType,
            UUID actorUserId, String actor, AuditSource source) {
        validateDateRange(from, to);
        PageRequest pageable = pageRequest(page, size, sortBy, sortDirection);
        Page<ActivityAuditListResponse> result = repository.findAll(
                ActivityAuditSpecifications.filteredBy(
                        from, to, action, entityType, actorUserId, actor, source), pageable)
                .map(mapper::toListResponse);
        return pagination(result);
    }

    public ActivityAuditDetailResponse findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDetailResponse)
                .orElseThrow(() -> new ActivityAuditLogNotFoundException(id));
    }

    public PaginationResponse<ActivityAuditListResponse> findEntityHistory(
            AuditEntityType entityType, String entityId, int page, int size) {
        if (entityType == null || entityId == null || entityId.isBlank() || entityId.length() > 100) {
            throw new IllegalArgumentException("A valid entity type and entity ID are required.");
        }
        PageRequest pageable = PageRequest.of(pageNumber(page), pageSize(size),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        Page<ActivityAuditListResponse> result = repository.findAll(
                ActivityAuditSpecifications.forEntity(entityType, entityId), pageable)
                .map(mapper::toListResponse);
        return pagination(result);
    }

    private PageRequest pageRequest(int page, int size, String sortBy, String sortDirection) {
        String field = sortBy == null || sortBy.isBlank() ? "occurredAt" : sortBy;
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported audit-log sort field.");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(
                    sortDirection == null || sortDirection.isBlank() ? "desc" : sortDirection.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
        return PageRequest.of(pageNumber(page), pageSize(size),
                Sort.by(new Sort.Order(direction, field), Sort.Order.desc("id")));
    }

    private int pageNumber(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must not be negative.");
        }
        return page;
    }

    private int pageSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        return size;
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The 'from' timestamp must not be after 'to'.");
        }
    }

    private PaginationResponse<ActivityAuditListResponse> pagination(Page<ActivityAuditListResponse> page) {
        return PaginationResponse.<ActivityAuditListResponse>builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .last(page.isLast())
                .build();
    }
}
