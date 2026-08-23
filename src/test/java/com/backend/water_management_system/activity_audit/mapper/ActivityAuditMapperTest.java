package com.backend.water_management_system.activity_audit.mapper;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.exception.ActivityAuditDataException;
import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.activity_audit.service.SafeChangedFieldsSerializer;
import com.backend.water_management_system.user.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityAuditMapperTest {

    private final ActivityAuditMapper mapper =
            new ActivityAuditMapper(new SafeChangedFieldsSerializer(new ObjectMapper()));

    @Test
    void detailReturnsSanitizedMapAndNeverRawJson() {
        var response = mapper.toDetailResponse(entry("{\"email\":\"changed\"}"));

        assertThat(response.changedFields()).containsEntry("email", "changed");
        assertThat(response.toString()).doesNotContain("private@example.test", "passwordHash", "token");
    }

    @Test
    void malformedJsonFailsWithControlledExceptionThatDoesNotLeakContent() {
        String malformed = "{private@example.test:raw-secret";

        assertThatThrownBy(() -> mapper.toDetailResponse(entry(malformed)))
                .isInstanceOf(ActivityAuditDataException.class)
                .hasMessageNotContaining("private@example.test")
                .hasMessageNotContaining("raw-secret");
    }

    private ActivityAuditLog entry(String changedFields) {
        return new ActivityAuditLog(
                Instant.now(),
                new AuditActorSnapshot(UUID.randomUUID(), "Administrator", Role.SUPER_ADMIN),
                AuditAction.USER_PROFILE_UPDATED,
                AuditEntityType.USER,
                UUID.randomUUID().toString(),
                "User account was updated.",
                changedFields,
                AuditSource.WEB);
    }
}
