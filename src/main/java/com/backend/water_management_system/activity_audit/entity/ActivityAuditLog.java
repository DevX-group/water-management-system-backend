package com.backend.water_management_system.activity_audit.entity;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.user.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(
        name = "activity_audit_logs",
        indexes = {
                @Index(name = "idx_activity_audit_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_activity_audit_entity_history",
                        columnList = "entity_type, entity_id, occurred_at"),
                @Index(name = "idx_activity_audit_actor", columnList = "actor_user_id, occurred_at"),
                @Index(name = "idx_activity_audit_action", columnList = "action, occurred_at"),
                @Index(name = "idx_activity_audit_source", columnList = "source, occurred_at")
        })
public class ActivityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "actor_display_name", nullable = false, updatable = false, length = 150)
    private String actorDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", updatable = false, length = 40)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, updatable = false, length = 40)
    private AuditEntityType entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 100)
    private String entityId;

    @Column(name = "summary", nullable = false, updatable = false, length = 500)
    private String summary;

    @Column(name = "changed_fields", updatable = false, columnDefinition = "text")
    private String changedFields;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 20)
    private AuditSource source;

    protected ActivityAuditLog() {
    }

    public ActivityAuditLog(
            Instant occurredAt,
            AuditActorSnapshot actor,
            AuditAction action,
            AuditEntityType entityType,
            String entityId,
            String summary,
            String changedFields,
            AuditSource source) {
        this.occurredAt = occurredAt;
        this.actorUserId = actor.userId();
        this.actorDisplayName = actor.displayName();
        this.actorRole = actor.role();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary;
        this.changedFields = changedFields;
        this.source = source;
    }

    public UUID getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getActorUserId() { return actorUserId; }
    public String getActorDisplayName() { return actorDisplayName; }
    public Role getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public AuditEntityType getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getSummary() { return summary; }
    public String getChangedFields() { return changedFields; }
    public AuditSource getSource() { return source; }
}
