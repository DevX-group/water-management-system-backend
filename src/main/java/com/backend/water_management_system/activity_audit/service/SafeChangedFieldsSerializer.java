package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SafeChangedFieldsSerializer {

    private static final Set<String> MARKER_ONLY_FIELDS = Set.of(
            "fullName", "email", "phoneNumber", "nic", "meterNumber");

    private static final Map<AuditAction, Set<String>> ALLOWED_FIELDS = Map.ofEntries(
            Map.entry(AuditAction.USER_CREATED, Set.of("role", "status")),
            Map.entry(AuditAction.USER_PROFILE_UPDATED, Set.of("fullName", "email", "phoneNumber", "nic")),
            Map.entry(AuditAction.USER_STATUS_CHANGED, Set.of("status")),
            Map.entry(AuditAction.USER_ROLE_CHANGED, Set.of("role")),
            Map.entry(AuditAction.USER_ACTIVATED, Set.of("status")),
            Map.entry(AuditAction.PAYMENT_CREATED, Set.of("amount", "paymentMethod", "paymentType", "status")),
            Map.entry(AuditAction.PAYMENT_UPDATED, Set.of("amount", "status")),
            Map.entry(AuditAction.PAYMENT_STATUS_CHANGED, Set.of("status")),
            Map.entry(AuditAction.PAYMENT_DELETED, Set.of("amount", "paymentMethod", "paymentType", "status")),
            Map.entry(AuditAction.METER_READING_CREATED,
                    Set.of("meterNumber", "previousReading", "currentReading", "usageUnits", "readingDate")),
            Map.entry(AuditAction.METER_READING_UPDATED,
                    Set.of("meterNumber", "previousReading", "currentReading", "usageUnits", "readingDate")));

    private final ObjectMapper objectMapper;

    public SafeChangedFieldsSerializer() {
        this(new ObjectMapper());
    }

    public SafeChangedFieldsSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(AuditAction action, Map<String, String> changedFields) {
        if (changedFields == null || changedFields.isEmpty()) {
            return null;
        }

        Set<String> allowed = ALLOWED_FIELDS.getOrDefault(action, Set.of());
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        changedFields.forEach((field, value) -> {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("Audit field is not allowed for " + action + ": " + field);
            }
            if (value == null || value.isBlank() || value.length() > 120) {
                throw new IllegalArgumentException("Audit field value is invalid: " + field);
            }
            if (MARKER_ONLY_FIELDS.contains(field) && !"changed".equals(value)) {
                throw new IllegalArgumentException("Audit field may only record a change marker: " + field);
            }
            if (!MARKER_ONLY_FIELDS.contains(field) && !isSafeValue(field, value)) {
                throw new IllegalArgumentException("Audit field value does not match its safe format: " + field);
            }
            sanitized.put(field, value);
        });

        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize sanitized audit fields", exception);
        }
    }

    public Map<String, String> deserialize(String changedFields) {
        if (changedFields == null || changedFields.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(changedFields, new TypeReference<LinkedHashMap<String, String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored audit fields are invalid", exception);
        }
    }

    private boolean isSafeValue(String field, String value) {
        return switch (field) {
            case "role", "status", "paymentMethod", "paymentType" ->
                    value.matches("[A-Z_]+(?: -> [A-Z_]+)?");
            case "amount" -> value.matches("[0-9]+(?:\\.[0-9]{1,2})?(?: -> [0-9]+(?:\\.[0-9]{1,2})?)?");
            case "previousReading", "currentReading", "usageUnits" ->
                    value.matches("[0-9]+(?: -> [0-9]+)?");
            case "readingDate" ->
                    value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}(?: -> [0-9]{4}-[0-9]{2}-[0-9]{2})?");
            default -> false;
        };
    }
}
