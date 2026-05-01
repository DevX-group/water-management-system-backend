package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.SentMessageHistoryDto;
import com.backend.water_management_system.entity.SentMessage;
import com.backend.water_management_system.repository.SentMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SentMessageService {

    private final SentMessageRepository sentMessageRepository;

    public SentMessageService(SentMessageRepository sentMessageRepository) {
        this.sentMessageRepository = sentMessageRepository;
    }

    public SentMessage save(SentMessage sentMessage) {
        return sentMessageRepository.save(sentMessage);
    }

    public List<SentMessageHistoryDto> getHistory() {
        return sentMessageRepository.findAllByOrderBySentDateDescSentTimeDescIdDesc()
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private SentMessageHistoryDto toHistoryDto(SentMessage entity) {
        SentMessageHistoryDto dto = new SentMessageHistoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setChannels(entity.getChannels());
        dto.setRecipients(entity.getRecipients());
        dto.setSentDate(entity.getSentDate());
        dto.setSentTime(entity.getSentTime());
        dto.setEmailSuccessRate(entity.getEmailSuccessRate());
        dto.setSmsSuccessRate(entity.getSmsSuccessRate());
        dto.setTotalEmailsSent(entity.getTotalEmailsSent());
        dto.setTotalEmailsFailed(entity.getTotalEmailsFailed());
        dto.setTotalEmailsDelivered(entity.getTotalEmailsDelivered());
        dto.setTotalSMSsSent(entity.getTotalSMSsSent());
        dto.setTotalSMSsFailed(entity.getTotalSMSsFailed());
        dto.setTotalSMSsDelivered(entity.getTotalSMSsDelivered());
        return dto;
    }
}
