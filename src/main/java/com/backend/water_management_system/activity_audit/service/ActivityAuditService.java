package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
public class ActivityAuditService {

    private final ActivityAuditLogRepository repository;
    private final AuditActorResolver actorResolver;
    private final SafeChangedFieldsSerializer changedFieldsSerializer;
    private final Clock clock;

    @Autowired
    public ActivityAuditService(
            ActivityAuditLogRepository repository,
            AuditActorResolver actorResolver,
            SafeChangedFieldsSerializer changedFieldsSerializer) {
        this(repository, actorResolver, changedFieldsSerializer, Clock.systemUTC());
    }

    ActivityAuditService(
            ActivityAuditLogRepository repository,
            AuditActorResolver actorResolver,
            SafeChangedFieldsSerializer changedFieldsSerializer,
            Clock clock) {
        this.repository = repository;
        this.actorResolver = actorResolver;
        this.changedFieldsSerializer = changedFieldsSerializer;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ActivityAuditLog recordAuthenticatedWeb(
            AuditAction action, AuditEntityType entityType, Object entityId,
            Map<String, String> changedFields) {
        return record(action, entityType, entityId, changedFields, AuditSource.WEB,
                actorResolver.resolveAuthenticatedWebActor());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ActivityAuditLog recordPublicWeb(
            AuditAction action, AuditEntityType entityType, Object entityId,
            Map<String, String> changedFields) {
        return record(action, entityType, entityId, changedFields, AuditSource.WEB,
                actorResolver.resolvePublicWebActor());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ActivityAuditLog recordSystem(
            AuditAction action, AuditEntityType entityType, Object entityId,
            Map<String, String> changedFields) {
        return record(action, entityType, entityId, changedFields, AuditSource.SYSTEM,
                actorResolver.resolveSystemActor());
    }

    private ActivityAuditLog record(
            AuditAction action, AuditEntityType entityType, Object rawEntityId,
            Map<String, String> changedFields, AuditSource source, AuditActorSnapshot actor) {
        if (action == null || entityType == null || rawEntityId == null) {
            throw new IllegalArgumentException("Audit action, entity type, and entity ID are required");
        }
        String entityId = rawEntityId.toString();
        if (entityId.isBlank() || entityId.length() > 100) {
            throw new IllegalArgumentException("Audit entity ID must contain 1 to 100 characters");
        }
        validateEntityId(entityType, entityId);
        String serializedChanges = changedFieldsSerializer.serialize(action, changedFields);
        ActivityAuditLog entry = new ActivityAuditLog(
                Instant.now(clock), actor, action, entityType, entityId,
                summaryFor(action, entityType, entityId), serializedChanges, source);
        return repository.save(entry);
    }

    private void validateEntityId(AuditEntityType entityType, String entityId) {
        try {
            switch (entityType) {
                case USER -> java.util.UUID.fromString(entityId);
                case METER_READING -> Long.parseLong(entityId);
                case PAYMENT -> {
                    if (!entityId.matches("[A-Za-z0-9-]+")) {
                        throw new IllegalArgumentException("Payment audit entity ID has an invalid format");
                    }
                }
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Audit entity ID has an invalid format for " + entityType, exception);
        }
    }

    private String summaryFor(AuditAction action, AuditEntityType entityType, String entityId) {
        String subject = switch (entityType) {
            case USER -> "User account";
            case PAYMENT -> "Payment";
            case METER_READING -> "Meter reading";
        };
        String activity = switch (action) {
            case USER_CREATED, PAYMENT_CREATED, METER_READING_CREATED -> "was created";
            case USER_PROFILE_UPDATED, PAYMENT_UPDATED, METER_READING_UPDATED -> "was updated";
            case USER_STATUS_CHANGED, PAYMENT_STATUS_CHANGED -> "changed status";
            case USER_ROLE_CHANGED -> "changed role";
            case USER_ACTIVATED -> "was activated";
            case PAYMENT_DELETED -> "was deleted";
        };
        return subject + " " + entityId + " " + activity + ".";
    }
}
