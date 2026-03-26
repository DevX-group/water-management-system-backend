package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.ScheduledMessageDto;
import com.backend.water_management_system.dto.ScheduledMessageDto.*;
import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.entity.TemplateSection;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ScheduledMessageService {

    private final ScheduledMessageRepository repository;

    public ScheduledMessageService(ScheduledMessageRepository repository) {
        this.repository = repository;
    }

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
            e.setSmsTemplate(toTemplateEntity(dto.getTemplates().getSms()));
            e.setEmailTemplate(toTemplateEntity(dto.getTemplates().getEmail()));
        }
    }

    private MessageTemplate toTemplateEntity(MessageTemplateDto dto) {
        if (dto == null)
            return null;
        MessageTemplate t = new MessageTemplate();
        t.setCustom(dto.getIsCustom() != null && dto.getIsCustom());
        t.setContent(dto.getContent());
        t.setSubject(dto.getSubject());

        if (dto.getSections() != null) {
            List<TemplateSection> sections = IntStream.range(0, dto.getSections().size())
                    .mapToObj(i -> {
                        TemplateSectionDto sd = dto.getSections().get(i);
                        TemplateSection section = new TemplateSection();
                        section.setSectionKey(sd.getId());
                        section.setName(sd.getName());
                        section.setContent(sd.getContent());
                        section.setSectionOrder(i);

                        section.setMessageTemplate(t);
                        
                        return section;
                    })
                    .collect(Collectors.toList());
            t.setSections(sections);
        }
        return t;
    }
}
