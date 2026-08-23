package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.exception.ActivityAuditLogNotFoundException;
import com.backend.water_management_system.activity_audit.mapper.ActivityAuditMapper;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import com.backend.water_management_system.user.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({ActivityAuditQueryService.class, ActivityAuditMapper.class,
        SafeChangedFieldsSerializer.class, ObjectMapper.class})
class ActivityAuditQueryServiceTest {

    @Autowired
    private ActivityAuditLogRepository repository;
    @Autowired
    private ActivityAuditQueryService service;

    private UUID adminId;
    private UUID userEntityId;
    private Instant middle;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userEntityId = UUID.randomUUID();
        middle = Instant.parse("2026-08-23T10:00:00Z");
        repository.save(entry(middle.minusSeconds(60), "Older Admin", adminId,
                AuditAction.USER_CREATED, AuditEntityType.USER, userEntityId.toString(), AuditSource.WEB));
        repository.save(entry(middle, "System Auditor", null,
                AuditAction.PAYMENT_STATUS_CHANGED, AuditEntityType.PAYMENT, "PAY-7", AuditSource.SYSTEM));
        repository.save(entry(middle.plusSeconds(60), "System Auditor", adminId,
                AuditAction.USER_STATUS_CHANGED, AuditEntityType.USER, userEntityId.toString(), AuditSource.WEB));
        repository.flush();
    }

    @Test
    void defaultsAndStableSortReturnNewestFirst() {
        var result = service.findAll(0, 20, "occurredAt", "desc",
                null, null, null, null, null, null, null);

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getContent()).extracting(item -> item.occurredAt())
                .containsExactly(middle.plusSeconds(60), middle, middle.minusSeconds(60));
    }

    @Test
    void individualAndCombinedFiltersAreAppliedInclusively() {
        var inclusive = service.findAll(0, 20, "occurredAt", "desc", middle, middle,
                AuditAction.PAYMENT_STATUS_CHANGED, AuditEntityType.PAYMENT,
                null, "system", AuditSource.SYSTEM);
        assertThat(inclusive.getContent()).singleElement()
                .satisfies(item -> assertThat(item.entityId()).isEqualTo("PAY-7"));

        assertThat(service.findAll(0, 20, "occurredAt", "desc", null, null,
                null, null, adminId, null, null).getTotalElements()).isEqualTo(2);
        assertThat(service.findAll(0, 20, "occurredAt", "desc", null, null,
                AuditAction.USER_CREATED, null, null, null, null).getTotalElements()).isEqualTo(1);
        assertThat(service.findAll(0, 20, "occurredAt", "desc", null, null,
                null, AuditEntityType.USER, null, null, null).getTotalElements()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidRangePaginationAndUnsafeSorting() {
        assertThatThrownBy(() -> service.findAll(0, 20, "occurredAt", "desc",
                middle.plusSeconds(1), middle, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAll(-1, 20, "occurredAt", "desc",
                null, null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAll(0, 0, "occurredAt", "desc",
                null, null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAll(0, 101, "occurredAt", "desc",
                null, null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAll(0, 20, "changedFields", "desc",
                null, null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAll(0, 20, "occurredAt", "sideways",
                null, null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detailAndNotFoundUseDedicatedResponses() {
        UUID id = service.findAll(0, 20, "occurredAt", "desc", null, null,
                AuditAction.USER_STATUS_CHANGED, null, null, null, null).getContent().get(0).id();
        assertThat(service.findById(id).id()).isEqualTo(id);
        assertThat(service.findById(id).changedFields()).containsEntry("status", "ACTIVE -> SUSPENDED");

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ActivityAuditLogNotFoundException.class);
    }

    @Test
    void entityHistoryIsIsolatedAndDoesNotRequireLiveBusinessEntity() {
        var result = service.findEntityHistory(AuditEntityType.USER, userEntityId.toString(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(item -> {
            assertThat(item.entityType()).isEqualTo(AuditEntityType.USER);
            assertThat(item.entityId()).isEqualTo(userEntityId.toString());
        });
        assertThat(result.getContent()).extracting(item -> item.occurredAt())
                .containsExactly(middle.plusSeconds(60), middle.minusSeconds(60));
    }

    private ActivityAuditLog entry(
            Instant occurredAt, String actorName, UUID actorId, AuditAction action,
            AuditEntityType entityType, String entityId, AuditSource source) {
        String changes = action == AuditAction.USER_CREATED
                ? "{\"role\":\"CUSTOMER\",\"status\":\"PENDING_ACTIVATION\"}"
                : "{\"status\":\"ACTIVE -> SUSPENDED\"}";
        return new ActivityAuditLog(
                occurredAt, new AuditActorSnapshot(actorId, actorName, actorId == null ? null : Role.SUPER_ADMIN),
                action, entityType, entityId, "Controlled summary.", changes, source);
    }
}
