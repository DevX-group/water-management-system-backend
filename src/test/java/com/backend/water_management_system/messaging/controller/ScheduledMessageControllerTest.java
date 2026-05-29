package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.ScheduledMessageDto;
import com.backend.water_management_system.messaging.dto.SentMessageFailureDto;
import com.backend.water_management_system.messaging.dto.SentMessageHistoryDto;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import com.backend.water_management_system.messaging.service.ScheduledMessageService;
import com.backend.water_management_system.messaging.service.SentMessageService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScheduledMessageControllerTest {

    @Mock
    private ScheduledMessageService service;

    @Mock
    private SentMessageService sentMessageService;

    @InjectMocks
    private ScheduledMessageController controller;

    @Test
    void getAll_returnsList() throws Exception {
        ScheduledMessageDto dto = buildScheduledMessageDto();
        dto.setId(10L);
        given(service.getAll()).willReturn(List.of(dto));

        ResponseEntity<List<ScheduledMessageDto>> response = controller.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(10L);
    }

    @Test
    void getHistory_returnsPage() throws Exception {
        SentMessageHistoryDto history = new SentMessageHistoryDto();
        history.setId(20L);
        history.setName("Monthly bill");
        given(sentMessageService.getHistory(0, 10)).willReturn(new PageImpl<>(List.of(history)));

        ResponseEntity<?> response = controller.getHistory(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getPlaceholders_returnsList() throws Exception {
        ResponseEntity<List<String>> response = controller.getPlaceholders();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void getById_returnsMessage() throws Exception {
        ScheduledMessageDto dto = buildScheduledMessageDto();
        dto.setId(5L);
        given(service.getById(5L)).willReturn(dto);

        ResponseEntity<ScheduledMessageDto> response = controller.getById(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(5L);
    }

    @Test
    void getFailures_returnsPage() throws Exception {
        SentMessageFailureDto failure = new SentMessageFailureDto();
        failure.setSubscriptionNumber("SUB-1");
        given(sentMessageService.getFailures(3L, 0, 5)).willReturn(new PageImpl<>(List.of(failure)));

        ResponseEntity<?> response = controller.getFailures(3L, 0, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void create_returnsSavedMessage() throws Exception {
        ScheduledMessageDto dto = buildScheduledMessageDto();
        ScheduledMessageDto saved = buildScheduledMessageDto();
        saved.setId(55L);
        given(service.create(any(ScheduledMessageDto.class))).willReturn(saved);

        ResponseEntity<ScheduledMessageDto> response = controller.create(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(55L);
    }

    @Test
    void update_returnsUpdatedMessage() throws Exception {
        ScheduledMessageDto dto = buildScheduledMessageDto();
        dto.setName("Updated name");
        given(service.update(eq(7L), any(ScheduledMessageDto.class))).willReturn(dto);

        ResponseEntity<ScheduledMessageDto> response = controller.update(7L, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated name");
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        ResponseEntity<Void> response = controller.delete(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(service).should().delete(9L);
    }

    private ScheduledMessageDto buildScheduledMessageDto() {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setName("Water bill reminder");
        dto.setChannels(List.of(MessageChannel.SMS, MessageChannel.EMAIL));
        dto.setRecipients(RecipientType.ALL_CUSTOMERS);
        dto.setIsDefault(false);

        ScheduledMessageDto.ScheduleDto schedule = new ScheduledMessageDto.ScheduleDto();
        schedule.setType(ScheduleType.ONE_TIME);
        schedule.setDate(LocalDate.now());
        schedule.setTime(LocalTime.NOON);
        dto.setSchedule(schedule);

        ScheduledMessageDto.MessageTemplateDto smsTemplate = new ScheduledMessageDto.MessageTemplateDto();
        smsTemplate.setIsCustom(true);
        smsTemplate.setContent("SMS content");

        ScheduledMessageDto.MessageTemplateDto emailTemplate = new ScheduledMessageDto.MessageTemplateDto();
        emailTemplate.setIsCustom(true);
        emailTemplate.setSubject("Email subject");
        emailTemplate.setContent("Email content");

        ScheduledMessageDto.TemplatesDto templates = new ScheduledMessageDto.TemplatesDto();
        templates.setSms(smsTemplate);
        templates.setEmail(emailTemplate);
        dto.setTemplates(templates);

        return dto;
    }
}
