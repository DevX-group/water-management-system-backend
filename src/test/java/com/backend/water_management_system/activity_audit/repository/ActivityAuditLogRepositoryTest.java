package com.backend.water_management_system.activity_audit.repository;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ActivityAuditLogRepositoryTest {

    @Autowired
    private ActivityAuditLogRepository repository;

    @Test
    void persistsGeneratedUuidInstantAndActorSnapshot() {
        UUID actorId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-23T10:15:30Z");
        ActivityAuditLog saved = repository.saveAndFlush(new ActivityAuditLog(
                occurredAt,
                new AuditActorSnapshot(actorId, "Audit Administrator", Role.SUPER_ADMIN),
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                UUID.randomUUID().toString(),
                "User account was created.",
                "{\"role\":\"CUSTOMER\"}",
                AuditSource.WEB));

        assertThat(saved.getId()).isNotNull();
        ActivityAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(reloaded.getOccurredAt().toString()).endsWith("Z");
        assertThat(reloaded.getActorUserId()).isEqualTo(actorId);
        assertThat(reloaded.getActorDisplayName()).isEqualTo("Audit Administrator");
        assertThat(reloaded.getActorRole()).isEqualTo(Role.SUPER_ADMIN);
    }

    @Test
    void persistsEveryAuditEnumValue() {
        int expected = 0;
        for (AuditAction action : AuditAction.values()) {
            repository.save(entry(action, AuditEntityType.USER, AuditSource.WEB, "user-" + expected++));
        }
        for (AuditEntityType entityType : AuditEntityType.values()) {
            repository.save(entry(AuditAction.USER_CREATED, entityType, AuditSource.SYSTEM, "entity-" + expected++));
        }
        for (AuditSource source : AuditSource.values()) {
            repository.save(entry(AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT, source, "payment-" + expected++));
        }

        repository.flush();
        assertThat(repository.count()).isEqualTo(expected);
        assertThat(repository.findAll()).extracting(ActivityAuditLog::getSource).contains(AuditSource.MCP);
    }

    @Test
    void supportsUuidStringAndLongEntityIdentifiers() {
        UUID uuid = UUID.randomUUID();
        repository.save(entry(AuditAction.USER_CREATED, AuditEntityType.USER, AuditSource.WEB, uuid.toString()));
        repository.save(entry(AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT, AuditSource.WEB, "PAY-42"));
        repository.save(entry(AuditAction.METER_READING_CREATED, AuditEntityType.METER_READING, AuditSource.WEB, "42"));

        assertThat(repository.findAll()).extracting(ActivityAuditLog::getEntityId)
                .containsExactlyInAnyOrder(uuid.toString(), "PAY-42", "42");
    }

    private ActivityAuditLog entry(
            AuditAction action, AuditEntityType entityType, AuditSource source, String entityId) {
        AuditActorSnapshot actor = source == AuditSource.SYSTEM
                ? new AuditActorSnapshot(null, "System", null)
                : new AuditActorSnapshot(UUID.randomUUID(), "Administrator", Role.SYSTEM_ADMIN);
        return new ActivityAuditLog(
                Instant.now(), actor, action, entityType, entityId,
                "Controlled summary.", null, source);
    }
}
