package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.messaging.dto.SentMessageFailureDto;
import com.backend.water_management_system.messaging.dto.SentMessageHistoryDto;
import com.backend.water_management_system.messaging.entity.SentMessage;
import com.backend.water_management_system.messaging.repository.SentMessageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SentMessageService {

    private final SentMessageRepository sentMessageRepository;

    public SentMessage save(SentMessage sentMessage) {
        return sentMessageRepository.save(sentMessage);
    }

    public Page<SentMessageHistoryDto> getHistory(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "sentDate", "sentTime", "id"));
        
                return sentMessageRepository.findAll(pageRequest)
                .map(this::toHistoryDto);
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

    public List<SentMessageFailureDto> getFailures(Long sentMessageId) {
        SentMessage message = sentMessageRepository.findById(sentMessageId)
                .orElseThrow();

        return message.getFailedRecipients().stream()
                .map(f -> {
                    SentMessageFailureDto dto = new SentMessageFailureDto();
                    dto.setSubscriptionNumber(f.getCustomer().getSubscriptionNumber());
                    dto.setCustomerName(f.getCustomer().getAccountHolderName());
                    dto.setPhoneNumber(f.getCustomer().getMobileNumber());
                    dto.setEmail(f.getCustomer().getEmail());
                    dto.setSmsFailed(f.isSmsFailed());
                    dto.setEmailFailed(f.isEmailFailed());
                    return dto;
                })
                .toList();
    }
}
