package com.backend.water_management_system.activity_audit.mapper;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.exception.ActivityAuditDataException;
import com.backend.water_management_system.activity_audit.service.SafeChangedFieldsSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ActivityAuditMapper {

    private final SafeChangedFieldsSerializer changedFieldsSerializer;

    public ActivityAuditMapper(SafeChangedFieldsSerializer changedFieldsSerializer) {
        this.changedFieldsSerializer = changedFieldsSerializer;
    }

    public ActivityAuditListResponse toListResponse(ActivityAuditLog entry) {
        return new ActivityAuditListResponse(
                entry.getId(), entry.getOccurredAt(), entry.getActorUserId(), entry.getActorDisplayName(),
                entry.getActorRole(), entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                entry.getSummary(), entry.getSource());
    }

    public ActivityAuditDetailResponse toDetailResponse(ActivityAuditLog entry) {
        Map<String, String> changedFields;
        try {
            changedFields = changedFieldsSerializer.deserialize(entry.getChangedFields());
        } catch (IllegalStateException exception) {
            log.error("Unable to deserialize changed fields for activity audit entry {}", entry.getId());
            throw new ActivityAuditDataException();
        }
        return new ActivityAuditDetailResponse(
                entry.getId(), entry.getOccurredAt(), entry.getActorUserId(), entry.getActorDisplayName(),
                entry.getActorRole(), entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                entry.getSummary(), changedFields, entry.getSource());
    }
}
