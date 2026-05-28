package com.backend.water_management_system.messaging.dto;

import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SentMessageHistoryDto {
    
    private Long id;
    private String name;
    private List<MessageChannel> channels;
    private RecipientType recipients;
    private LocalDate sentDate;
    private LocalTime sentTime;
    private Double emailSuccessRate;
    private Double smsSuccessRate;
    private Integer totalEmailsSent;
    private Integer totalEmailsFailed;
    private Integer totalEmailsDelivered;
    private Integer totalSMSsSent;
    private Integer totalSMSsFailed;
    private Integer totalSMSsDelivered;
}
