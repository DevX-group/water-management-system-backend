package com.backend.water_management_system.messaging.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SentMessageHistoryDto {
    
    private Long id;
    private String name;
    private String channels;
    private String recipients;
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
