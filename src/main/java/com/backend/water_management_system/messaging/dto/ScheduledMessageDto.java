package com.backend.water_management_system.messaging.dto;

import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ScheduledMessageDto {

    private Long id;
    private String name;
    private List<MessageChannel> channels;
    private ScheduleDto schedule;
    private RecipientType recipients;
    private TemplatesDto templates;
    private Boolean isDefault;

    // --- Nested DTOs ---

    @NoArgsConstructor
    @Getter
    @Setter
    public static class ScheduleDto {
        private ScheduleType type;
        private Integer dayOfMonth;
        private LocalDate date;
        private LocalTime time;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class TemplateSectionDto {
        private String id;
        private String name;
        private String content;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class MessageTemplateDto {
        // Using Boolean (boxed) + getIsCustom() so Jackson maps to JSON key "isCustom"
        private Boolean isCustom;
        private String content;
        private String subject;
        private List<TemplateSectionDto> sections;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class TemplatesDto {
        private MessageTemplateDto sms;
        private MessageTemplateDto email;
    }
}
