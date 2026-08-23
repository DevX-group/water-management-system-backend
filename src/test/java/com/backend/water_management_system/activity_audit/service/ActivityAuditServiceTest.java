package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import com.backend.water_management_system.user.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAuditServiceTest {

    @Mock
    private ActivityAuditLogRepository repository;
    @Mock
    private AuditActorResolver actorResolver;

    @Test
    void recordsAuthenticatedActorAtUtcInstantWithoutAcceptingActorBusinessData() {
        Instant now = Instant.parse("2026-08-23T05:45:00Z");
        UUID actorId = UUID.randomUUID();
        when(actorResolver.resolveAuthenticatedWebActor())
                .thenReturn(new AuditActorSnapshot(actorId, "Administrator", Role.SUPER_ADMIN));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ActivityAuditService service = service(now);

        ActivityAuditLog result = service.recordAuthenticatedWeb(
                AuditAction.USER_STATUS_CHANGED, AuditEntityType.USER, UUID.randomUUID(),
                Map.of("status", "ACTIVE -> SUSPENDED"));

        assertThat(result.getOccurredAt()).isEqualTo(now);
        assertThat(result.getActorUserId()).isEqualTo(actorId);
        assertThat(result.getActorDisplayName()).isEqualTo("Administrator");
        assertThat(result.getSource()).isEqualTo(AuditSource.WEB);
        assertThat(result.getChangedFields()).isEqualTo("{\"status\":\"ACTIVE -> SUSPENDED\"}");
    }

    @Test
    void publicAndSystemMethodsChooseTheirOwnControlledActors() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actorResolver.resolvePublicWebActor())
                .thenReturn(new AuditActorSnapshot(null, "Unauthenticated user", null));
        when(actorResolver.resolveSystemActor())
                .thenReturn(new AuditActorSnapshot(null, "System", null));
        ActivityAuditService service = service(Instant.now());

        ActivityAuditLog publicEntry = service.recordPublicWeb(
                AuditAction.USER_ACTIVATED, AuditEntityType.USER, UUID.randomUUID(),
                Map.of("status", "PENDING_ACTIVATION -> ACTIVE"));
        ActivityAuditLog systemEntry = service.recordSystem(
                AuditAction.PAYMENT_STATUS_CHANGED, AuditEntityType.PAYMENT, "PAY-1",
                Map.of("status", "PENDING -> FULL"));

        assertThat(publicEntry.getActorDisplayName()).isEqualTo("Unauthenticated user");
        assertThat(publicEntry.getSource()).isEqualTo(AuditSource.WEB);
        assertThat(systemEntry.getActorDisplayName()).isEqualTo("System");
        assertThat(systemEntry.getSource()).isEqualTo(AuditSource.SYSTEM);
    }

    @Test
    void rejectsSensitiveOrNonAllowlistedChangedFields() {
        when(actorResolver.resolveSystemActor()).thenReturn(new AuditActorSnapshot(null, "System", null));
        ActivityAuditService service = service(Instant.now());

        assertThatThrownBy(() -> service.recordSystem(
                AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER, UUID.randomUUID(),
                Map.of("email", "private@example.test")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recordSystem(
                AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER, UUID.randomUUID(),
                Map.of("passwordHash", "secret")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recordSystem(
                AuditAction.USER_STATUS_CHANGED, AuditEntityType.USER, UUID.randomUUID(),
                Map.of("status", "ACTIVE -> private@example.test")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recordSystem(
                AuditAction.USER_CREATED, AuditEntityType.USER, "998877665V",
                Map.of("role", "CUSTOMER")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serializesAndDeserializesOnlySafeFields() {
        SafeChangedFieldsSerializer serializer = new SafeChangedFieldsSerializer(new ObjectMapper());
        String json = serializer.serialize(AuditAction.USER_PROFILE_UPDATED,
                Map.of("email", "changed", "phoneNumber", "changed"));

        assertThat(serializer.deserialize(json))
                .containsExactlyInAnyOrderEntriesOf(Map.of("email", "changed", "phoneNumber", "changed"));
    }

    @Test
    void serviceExposesNoUpdateOrDeleteOperation() {
        assertThat(ActivityAuditService.class.getDeclaredMethods())
                .filteredOn(method -> Modifier.isPublic(method.getModifiers()))
                .extracting(Method::getName)
                .doesNotContain("update", "delete", "save", "remove");
    }

    @Test
    void generatedSummaryUsesControlledDataRatherThanBusinessPayload() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actorResolver.resolveSystemActor()).thenReturn(new AuditActorSnapshot(null, "System", null));
        ActivityAuditService service = service(Instant.now());

        service.recordSystem(AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT, "PAY-9",
                Map.of("amount", "100.00"));

        ArgumentCaptor<ActivityAuditLog> captor = ArgumentCaptor.forClass(ActivityAuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSummary()).isEqualTo("Payment PAY-9 was created.");
    }

    private ActivityAuditService service(Instant instant) {
        return new ActivityAuditService(
                repository,
                actorResolver,
                new SafeChangedFieldsSerializer(new ObjectMapper()),
                Clock.fixed(instant, ZoneOffset.UTC));
    }
}
