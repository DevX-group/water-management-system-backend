package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.messaging.dto.SentMessageFailureDto;
import com.backend.water_management_system.messaging.dto.SentMessageHistoryDto;
import com.backend.water_management_system.messaging.entity.SentMessage;
import com.backend.water_management_system.messaging.entity.SentMessageFailure;
import com.backend.water_management_system.messaging.exceptions.MessagingNotFoundException;
import com.backend.water_management_system.messaging.repository.SentMessageFailureRepository;
import com.backend.water_management_system.messaging.repository.SentMessageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SentMessageService {

    private final SentMessageRepository sentMessageRepository;
    private final SentMessageFailureRepository sentMessageFailureRepository;

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

    public Page<SentMessageFailureDto> getFailures(Long sentMessageId, int page, int size) {
        sentMessageRepository.findById(sentMessageId)
                .orElseThrow(() -> new MessagingNotFoundException("Unable to find this message history."));

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id"));

        return sentMessageFailureRepository.findBySentMessageId(sentMessageId, pageRequest)
                .map(this::toFailureDto);
    }

    private SentMessageFailureDto toFailureDto(SentMessageFailure entity) {
        SentMessageFailureDto dto = new SentMessageFailureDto();
        dto.setSubscriptionNumber(entity.getCustomer().getSubscriptionNumber());
        dto.setCustomerName(entity.getCustomer().getAccountHolderName());
        dto.setPhoneNumber(entity.getCustomer().getMobileNumber());
        dto.setEmail(entity.getCustomer().getEmail());
        dto.setSmsFailed(entity.isSmsFailed());
        dto.setEmailFailed(entity.isEmailFailed());
        return dto;
    }
}
