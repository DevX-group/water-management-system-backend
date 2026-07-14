package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.MessagingEnumsResponse;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.MessagePlaceholder;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import com.backend.water_management_system.messaging.enums.TriggerType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messaging/enums")
@CrossOrigin
public class MessagingEnumsController {

    @GetMapping
    public ResponseEntity<MessagingEnumsResponse> getEnums() {
        List<String> triggerTypes = Arrays.stream(TriggerType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new MessagingEnumsResponse(
                MessageChannel.labels(),
                ScheduleType.labels(),
                RecipientType.labels(),
                MessagePlaceholder.keys(),
                triggerTypes));
    }
}
