package com.backend.water_management_system.messaging.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum ScheduleType {
    RECURRING("Recurring"),
    ONE_TIME("One-Time");

    private final String label;

    ScheduleType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // to get the enum from a string equals to the label
    @JsonCreator
    public static ScheduleType fromLabel(String value) {
        if (value == null) {
            return null;
        }

        String normalized = normalize(value);

        if ("recurring".equals(normalized)) {
            return RECURRING;
        }

        if ("one time".equals(normalized) || "onetime".equals(normalized)) {
            return ONE_TIME;
        }

        return null;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(ScheduleType::getLabel).toList();
    }

    private static String normalize(String value) {
        String lowered = value.trim().toLowerCase();
        String withSpaces = lowered.replace('_', ' ').replace('-', ' ');
        return withSpaces.replaceAll("\\s+", " ");
    }
}
