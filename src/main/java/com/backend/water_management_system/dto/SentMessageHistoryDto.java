package com.backend.water_management_system.dto;

import java.time.LocalDate;
import java.time.LocalTime;

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

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public LocalDate getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDate sentDate) {
        this.sentDate = sentDate;
    }

    public LocalTime getSentTime() {
        return sentTime;
    }

    public void setSentTime(LocalTime sentTime) {
        this.sentTime = sentTime;
    }

    public Double getEmailSuccessRate() {
        return emailSuccessRate;
    }

    public void setEmailSuccessRate(Double emailSuccessRate) {
        this.emailSuccessRate = emailSuccessRate;
    }

    public Double getSmsSuccessRate() {
        return smsSuccessRate;
    }

    public void setSmsSuccessRate(Double smsSuccessRate) {
        this.smsSuccessRate = smsSuccessRate;
    }

    public Integer getTotalEmailsSent() {
        return totalEmailsSent;
    }

    public void setTotalEmailsSent(Integer totalEmailsSent) {
        this.totalEmailsSent = totalEmailsSent;
    }

    public Integer getTotalEmailsFailed() {
        return totalEmailsFailed;
    }

    public void setTotalEmailsFailed(Integer totalEmailsFailed) {
        this.totalEmailsFailed = totalEmailsFailed;
    }

    public Integer getTotalEmailsDelivered() {
        return totalEmailsDelivered;
    }

    public void setTotalEmailsDelivered(Integer totalEmailsDelivered) {
        this.totalEmailsDelivered = totalEmailsDelivered;
    }

    public Integer getTotalSMSsSent() {
        return totalSMSsSent;
    }

    public void setTotalSMSsSent(Integer totalSMSsSent) {
        this.totalSMSsSent = totalSMSsSent;
    }

    public Integer getTotalSMSsFailed() {
        return totalSMSsFailed;
    }

    public void setTotalSMSsFailed(Integer totalSMSsFailed) {
        this.totalSMSsFailed = totalSMSsFailed;
    }

    public Integer getTotalSMSsDelivered() {
        return totalSMSsDelivered;
    }

    public void setTotalSMSsDelivered(Integer totalSMSsDelivered) {
        this.totalSMSsDelivered = totalSMSsDelivered;
    }
}
