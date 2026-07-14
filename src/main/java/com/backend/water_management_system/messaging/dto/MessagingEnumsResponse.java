package com.backend.water_management_system.messaging.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessagingEnumsResponse {
    
    private final List<String> channels;
    private final List<String> scheduleTypes;
    private final List<String> recipientTypes;
    private final List<String> placeholders;
    private final List<String> triggerTypes;
}
