package com.backend.water_management_system.activity_audit.repository;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class ActivityAuditSpecifications {

    private ActivityAuditSpecifications() {
    }

    public static Specification<ActivityAuditLog> filteredBy(
            Instant from, Instant to, AuditAction action, AuditEntityType entityType,
            UUID actorUserId, String actor, AuditSource source) {
        Specification<ActivityAuditLog> specification = Specification.unrestricted();
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        if (action != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (entityType != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (actorUserId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (actor != null && !actor.isBlank()) {
            String pattern = "%" + escapeLike(actor.trim().toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("actorDisplayName")), pattern, '\\'));
        }
        if (source != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("source"), source));
        }
        return specification;
    }

    public static Specification<ActivityAuditLog> forEntity(AuditEntityType entityType, String entityId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("entityType"), entityType),
                cb.equal(root.get("entityId"), entityId));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
