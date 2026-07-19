package com.backend.water_management_system.messaging.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum TriggerType {
    PAYMENT_CONFIRMED("Payment Confirmed"),
    BANK_SLIP_REJECTED("Bank Slip Rejected"),
    EMAIL_VERIFICATION("Email Verification"),
    PHONE_VERIFICATION("Phone Verification");

    private final String label;

    TriggerType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // to get the enum from a string equals to the label
    @JsonCreator
    public static TriggerType fromLabel(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        for (TriggerType type : values()) {
            if (type.label.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }

        String enumNameStyle = normalized.replace(' ', '_');
        for (TriggerType type : values()) {
            if (type.name().equalsIgnoreCase(enumNameStyle)) {
                return type;
            }
        }

        return null;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(TriggerType::getLabel).toList();
    }
}
