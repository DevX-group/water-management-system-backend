package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.MessagingEnumsResponse;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.MessagePlaceholder;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingEnumsControllerTest {

    @Test
    void getEnums_returnsLabels() {
        MessagingEnumsController controller = new MessagingEnumsController();

        ResponseEntity<MessagingEnumsResponse> response = controller.getEnums();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessagingEnumsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getChannels()).hasSize(MessageChannel.labels().size());
        assertThat(body.getScheduleTypes()).hasSize(ScheduleType.labels().size());
        assertThat(body.getRecipientTypes()).hasSize(RecipientType.labels().size());
        assertThat(body.getPlaceholders()).hasSize(MessagePlaceholder.keys().size());
    }
}
