package com.backend.water_management_system.messaging.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum MessageChannel {
    SMS("SMS"),
    EMAIL("Email");

    private final String label;

    MessageChannel(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // to get the enum from a string equals to the label
    @JsonCreator
    public static MessageChannel fromLabel(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        for (MessageChannel channel : values()) {
            if (channel.label.equalsIgnoreCase(normalized)) {
                return channel;
            }
        }

        return null;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(MessageChannel::getLabel).toList();
    }
}
