package com.backend.water_management_system.dto;

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
    private List<String> channels;
    private ScheduleDto schedule;
    private String recipients;
    private TemplatesDto templates;
    private Boolean isDefault;

    // --- Nested DTOs ---

    @NoArgsConstructor
    @Getter
    @Setter
    public static class ScheduleDto {
        private String type;
        private Integer dayOfMonth;
        private LocalDate date;
        private LocalTime time;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getDayOfMonth() {
            return dayOfMonth;
        }

        public void setDayOfMonth(Integer dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class TemplateSectionDto {
        private String id;
        private String name;
        private String content;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
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

        public Boolean getIsCustom() {
            return isCustom;
        }

        public void setIsCustom(Boolean isCustom) {
            this.isCustom = isCustom;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public List<TemplateSectionDto> getSections() {
            return sections;
        }

        public void setSections(List<TemplateSectionDto> sections) {
            this.sections = sections;
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class TemplatesDto {
        private MessageTemplateDto sms;
        private MessageTemplateDto email;

        public MessageTemplateDto getSms() {
            return sms;
        }

        public void setSms(MessageTemplateDto sms) {
            this.sms = sms;
        }

        public MessageTemplateDto getEmail() {
            return email;
        }

        public void setEmail(MessageTemplateDto email) {
            this.email = email;
        }
    }

    // --- Main class getters/setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public ScheduleDto getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleDto schedule) {
        this.schedule = schedule;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public TemplatesDto getTemplates() {
        return templates;
    }

    public void setTemplates(TemplatesDto templates) {
        this.templates = templates;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
