package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.TriggeredMessageDto;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.messaging.service.TriggeredMessageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TriggeredMessageControllerTest {

    @Mock
    private TriggeredMessageService service;

    @InjectMocks
    private TriggeredMessageController controller;

    @Test
    void getAll_returnsList() throws Exception {
        TriggeredMessageDto dto = buildTriggeredMessageDto();
        dto.setId(11L);
        given(service.getAll()).willReturn(List.of(dto));

        ResponseEntity<List<TriggeredMessageDto>> response = controller.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(11L);
    }

    @Test
    void getById_returnsMessage() throws Exception {
        TriggeredMessageDto dto = buildTriggeredMessageDto();
        dto.setId(12L);
        given(service.getById(12L)).willReturn(dto);

        ResponseEntity<TriggeredMessageDto> response = controller.getById(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(12L);
    }

    @Test
    void create_returnsSavedMessage() throws Exception {
        TriggeredMessageDto dto = buildTriggeredMessageDto();
        TriggeredMessageDto saved = buildTriggeredMessageDto();
        saved.setId(21L);
        given(service.create(any(TriggeredMessageDto.class))).willReturn(saved);

        ResponseEntity<TriggeredMessageDto> response = controller.create(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(21L);
    }

    @Test
    void update_returnsUpdatedMessage() throws Exception {
        TriggeredMessageDto dto = buildTriggeredMessageDto();
        dto.setName("Updated name");
        given(service.update(eq(7L), any(TriggeredMessageDto.class))).willReturn(dto);

        ResponseEntity<TriggeredMessageDto> response = controller.update(7L, dto);

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

    private TriggeredMessageDto buildTriggeredMessageDto() {
        TriggeredMessageDto dto = new TriggeredMessageDto();
        dto.setName("Payment confirmation");
        dto.setChannels(List.of(MessageChannel.SMS, MessageChannel.EMAIL));
        dto.setRecipients(RecipientType.ALL_CUSTOMERS);
        dto.setIsDefault(false);
        dto.setActive(true);
        dto.setTriggerType(TriggerType.PAYMENT_CONFIRMED);

        TriggeredMessageDto.MessageTemplateDto smsTemplate = new TriggeredMessageDto.MessageTemplateDto();
        smsTemplate.setIsCustom(true);
        smsTemplate.setContent("SMS content");

        TriggeredMessageDto.MessageTemplateDto emailTemplate = new TriggeredMessageDto.MessageTemplateDto();
        emailTemplate.setIsCustom(true);
        emailTemplate.setSubject("Email subject");
        emailTemplate.setContent("Email content");

        TriggeredMessageDto.TemplatesDto templates = new TriggeredMessageDto.TemplatesDto();
        templates.setSms(smsTemplate);
        templates.setEmail(emailTemplate);
        dto.setTemplates(templates);

        return dto;
    }
}
