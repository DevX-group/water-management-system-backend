package com.backend.water_management_system.messaging.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum RecipientType {
    ALL_CUSTOMERS("All Customers"),
    OVERDUE_CUSTOMERS("Overdue Customers");

    private final String label;

    RecipientType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // to get the enum from a string equals to the label
    @JsonCreator
    public static RecipientType fromLabel(String value) {
        if (value == null) {
            return ALL_CUSTOMERS;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.contains("overdue")) {
            return OVERDUE_CUSTOMERS;
        }

        return ALL_CUSTOMERS;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(RecipientType::getLabel).toList();
    }
}
