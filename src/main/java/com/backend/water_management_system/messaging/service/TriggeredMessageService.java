package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.messaging.dto.TriggeredMessageDto;
import com.backend.water_management_system.messaging.dto.TriggeredMessageDto.*;
import com.backend.water_management_system.messaging.entity.MessageTemplate;
import com.backend.water_management_system.messaging.entity.TemplateSection;
import com.backend.water_management_system.messaging.entity.TriggeredMessage;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.messaging.repository.TriggeredMessageRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TriggeredMessageService {

    private final TriggeredMessageRepository repository;

    public List<TriggeredMessageDto> getAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TriggeredMessageDto getById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));
    }

    @Transactional
    public TriggeredMessageDto create(TriggeredMessageDto dto) {
        TriggeredMessage entity = toEntity(dto);
        return toDto(repository.save(entity));
    }

    @Transactional
    public TriggeredMessageDto update(Long id, TriggeredMessageDto dto) {
        TriggeredMessage existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));

        updateEntity(existing, dto);
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private TriggeredMessageDto toDto(TriggeredMessage e) {
        TriggeredMessageDto dto = new TriggeredMessageDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setRecipients(e.getRecipients());
        dto.setIsDefault(e.isDefault());
        dto.setActive(e.isActive());
        dto.setTriggerType(e.getTriggerType() != null ? e.getTriggerType().name() : null);

        if (e.getChannels() != null && !e.getChannels().isEmpty()) {
            dto.setChannels(Arrays.asList(e.getChannels().split(",")));
        }

        TemplatesDto templates = new TemplatesDto();
        templates.setSms(toTemplateDto(e.getSmsTemplate()));
        templates.setEmail(toTemplateDto(e.getEmailTemplate()));
        dto.setTemplates(templates);

        return dto;
    }

    private MessageTemplateDto toTemplateDto(MessageTemplate t) {
        if (t == null) {
            return null;
        }
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

    private TriggeredMessage toEntity(TriggeredMessageDto dto) {
        TriggeredMessage e = new TriggeredMessage();
        updateEntity(e, dto);
        return e;
    }

    private void updateEntity(TriggeredMessage e, TriggeredMessageDto dto) {
        e.setName(dto.getName());
        e.setRecipients(dto.getRecipients());
        e.setDefault(dto.getIsDefault() != null && dto.getIsDefault());
        e.setActive(dto.getActive() != null && dto.getActive());
        e.setTriggerType(dto.getTriggerType() != null ? TriggerType.valueOf(dto.getTriggerType()) : null);

        if (dto.getChannels() != null) {
            e.setChannels(String.join(",", dto.getChannels()));
        }

        if (dto.getTemplates() != null) {
            e.setSmsTemplate(mergeTemplate(e.getSmsTemplate(), dto.getTemplates().getSms()));
            e.setEmailTemplate(mergeTemplate(e.getEmailTemplate(), dto.getTemplates().getEmail()));
        }
    }

    private MessageTemplate mergeTemplate(MessageTemplate existingTemplate, MessageTemplateDto dto) {
        if (dto == null) {
            return null;
        }

        MessageTemplate target = (existingTemplate != null) ? existingTemplate : new MessageTemplate();

        target.setCustom(dto.getIsCustom() != null && dto.getIsCustom());
        target.setContent(dto.getContent());
        target.setSubject(dto.getSubject());

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
