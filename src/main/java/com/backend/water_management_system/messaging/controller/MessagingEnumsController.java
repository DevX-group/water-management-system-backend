package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import java.util.List;
import lombok.Getter;
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

    @Getter
    public static class MessagingEnumsResponse {
        private final List<String> channels;
        private final List<String> scheduleTypes;
        private final List<String> recipientTypes;

        public MessagingEnumsResponse(List<String> channels, List<String> scheduleTypes, List<String> recipientTypes) {
            this.channels = channels;
            this.scheduleTypes = scheduleTypes;
            this.recipientTypes = recipientTypes;
        }
    }
}
