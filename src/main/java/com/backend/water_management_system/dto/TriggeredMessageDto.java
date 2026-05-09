package com.backend.water_management_system.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TriggeredMessageDto {

    private Long id;
    private String name;
    private List<String> channels;
    private String recipients;
    private TemplatesDto templates;
    private Boolean isDefault;
    private String triggerType;
    private Boolean active;

    // --- Nested DTOs ---

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
