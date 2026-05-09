package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.ScheduledMessageDto;
import com.backend.water_management_system.dto.ScheduledMessageDto.*;
import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.entity.TemplateSection;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledMessageService {

    private final ScheduledMessageRepository repository;

    public List<ScheduledMessageDto> getAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ScheduledMessageDto getById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));
    }

    @Transactional
    public ScheduledMessageDto create(ScheduledMessageDto dto) {
        ScheduledMessage entity = toEntity(dto);
        return toDto(repository.save(entity));
    }

    @Transactional
    public ScheduledMessageDto update(Long id, ScheduledMessageDto dto) {
        ScheduledMessage existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));

        updateEntity(existing, dto);
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    @Transactional
    public void saveAll(List<ScheduledMessageDto> messages) {
        messages.forEach(dto -> repository.save(toEntity(dto)));
    }

    // ---- Mapping helpers ----

    private ScheduledMessageDto toDto(ScheduledMessage e) {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setRecipients(e.getRecipients());
        dto.setIsDefault(e.isDefault());

        // Channels: "SMS,Email" -> ["SMS", "Email"]
        if (e.getChannels() != null && !e.getChannels().isEmpty()) {
            dto.setChannels(Arrays.asList(e.getChannels().split(",")));
        }

        // Schedule
        ScheduleDto schedule = new ScheduleDto();
        schedule.setType(e.getScheduleType());
        schedule.setDayOfMonth(e.getScheduleDayOfMonth());
        schedule.setDate(e.getScheduleDate());
        schedule.setTime(e.getScheduleTime());
        dto.setSchedule(schedule);

        // Templates
        TemplatesDto templates = new TemplatesDto();
        templates.setSms(toTemplateDto(e.getSmsTemplate()));
        templates.setEmail(toTemplateDto(e.getEmailTemplate()));
        dto.setTemplates(templates);

        return dto;
    }

    private MessageTemplateDto toTemplateDto(MessageTemplate t) {
        if (t == null)
            return null;
        MessageTemplateDto dto = new MessageTemplateDto();
        dto.setIsCustom(t.isCustom());
        dto.setContent(t.getContent() != null ? t.getContent() : "");
        dto.setSubject(t.getSubject());

        List<TemplateSectionDto> sections = t.getSections().stream()
                .map(s -> {
                    TemplateSectionDto sd = new TemplateSectionDto();
                    sd.setId(s.getSectionKey() != null ? s.getSectionKey() : String.valueOf(s.getId()));
                    sd.setName(s.getName());
                    sd.setContent(s.getContent() != null ? s.getContent() : "");
                    return sd;
                })
                .collect(Collectors.toList());
        dto.setSections(sections);
        return dto;
    }

    private ScheduledMessage toEntity(ScheduledMessageDto dto) {
        ScheduledMessage e = new ScheduledMessage();
        updateEntity(e, dto);
        return e;
    }

    private void updateEntity(ScheduledMessage e, ScheduledMessageDto dto) {
        String oldScheduleType = e.getScheduleType();
        Integer oldDayOfMonth = e.getScheduleDayOfMonth();
        java.time.LocalDate oldDate = e.getScheduleDate();
        java.time.LocalTime oldTime = e.getScheduleTime();
        String oldChannels = e.getChannels();
        String oldRecipients = e.getRecipients();

        e.setName(dto.getName());
        e.setRecipients(dto.getRecipients());
        e.setDefault(dto.getIsDefault() != null && dto.getIsDefault());

        // Channels list -> comma-separated string
        if (dto.getChannels() != null) {
            e.setChannels(String.join(",", dto.getChannels()));
        }

        // Schedule
        if (dto.getSchedule() != null) {
            ScheduleDto s = dto.getSchedule();
            e.setScheduleType(s.getType());
            e.setScheduleDayOfMonth(s.getDayOfMonth());
            e.setScheduleDate(s.getDate());
            e.setScheduleTime(s.getTime());
        }

        // Templates
        if (dto.getTemplates() != null) {
            // Update SMS and Email Templates instead of replacing
            e.setSmsTemplate(mergeTemplate(e.getSmsTemplate(), dto.getTemplates().getSms()));
            e.setEmailTemplate(mergeTemplate(e.getEmailTemplate(), dto.getTemplates().getEmail()));
        }

        // checks if something related to scheduling is updated (either in a recurring
        // or one-time message)
        boolean scheduleOrTargetingChanged = !Objects.equals(oldScheduleType, e.getScheduleType())
                || !Objects.equals(oldDayOfMonth, e.getScheduleDayOfMonth())
                || !Objects.equals(oldDate, e.getScheduleDate())
                || !Objects.equals(oldTime, e.getScheduleTime())
                || !Objects.equals(oldChannels, e.getChannels())
                || !Objects.equals(oldRecipients, e.getRecipients());

        // if yes, reset lastMessageSentAt or oneTimeMessageSent properties
        if (scheduleOrTargetingChanged) {
            e.setLastMessageSentAt(null);
            e.setOneTimeMessageSent(false);
        }
    }

    private MessageTemplate mergeTemplate(MessageTemplate existingTemplate, MessageTemplateDto dto) {
        if (dto == null)
            return null;

        MessageTemplate target = (existingTemplate != null) ? existingTemplate : new MessageTemplate();

        target.setCustom(dto.getIsCustom() != null && dto.getIsCustom());
        target.setContent(dto.getContent());
        target.setSubject(dto.getSubject());

        // Always replace sections fully so removed ones are deleted from DB.
        if (target.getSections() != null) {
            target.getSections().forEach(section -> section.setMessageTemplate(null));
            target.getSections().clear();
        }

        List<TemplateSectionDto> incomingSections = dto.getSections() != null
                ? dto.getSections()
                : Collections.emptyList();

        for (int i = 0; i < incomingSections.size(); i++) {
            TemplateSectionDto sd = incomingSections.get(i);
            TemplateSection section = new TemplateSection();

            section.setSectionKey(sd.getId());
            section.setName(sd.getName());
            section.setContent(sd.getContent());
            section.setSectionOrder(i);

            section.setMessageTemplate(target);
            target.getSections().add(section);
        }
        return target;
    }
}
