package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.MessagingEnumsResponse;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging/enums")
@CrossOrigin
public class MessagingEnumsController {

    @GetMapping
    public ResponseEntity<MessagingEnumsResponse> getEnums() {
        return ResponseEntity.ok(new MessagingEnumsResponse(
                MessageChannel.labels(),
                ScheduleType.labels(),
                RecipientType.labels()));
    }
}
