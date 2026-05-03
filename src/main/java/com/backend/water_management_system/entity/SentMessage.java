package com.backend.water_management_system.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sent_messages")
public class SentMessage extends Message {

    @Column(name = "source_scheduled_message_id")
    private Long sourceScheduledMessageId;

    @Column(name = "sent_date")
    private LocalDate sentDate;

    @Column(name = "sent_time")
    private LocalTime sentTime;

    @Column(name = "email_success_rate")
    private Double emailSuccessRate;

    @Column(name = "sms_success_rate")
    private Double smsSuccessRate;

    @Column(name = "total_emails_sent")
    private Integer totalEmailsSent;

    @Column(name = "total_emails_failed")
    private Integer totalEmailsFailed;

    @Column(name = "total_emails_delivered")
    private Integer totalEmailsDelivered;

    @Column(name = "total_smss_sent")
    private Integer totalSMSsSent;

    @Column(name = "total_smss_failed")
    private Integer totalSMSsFailed;

    @Column(name = "total_smss_delivered")
    private Integer totalSMSsDelivered;

    @OneToMany(mappedBy = "sentMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentMessageFailure> failedRecipients = new ArrayList<>();

    public LocalDate getSentDate() {
        return sentDate;
    }

    public Long getSourceScheduledMessageId() {
        return sourceScheduledMessageId;
    }

    public void setSourceScheduledMessageId(Long sourceScheduledMessageId) {
        this.sourceScheduledMessageId = sourceScheduledMessageId;
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

    public List<SentMessageFailure> getFailedRecipients() {
        return failedRecipients;
    }

    public void setFailedRecipients(List<SentMessageFailure> failedRecipients) {
        this.failedRecipients = failedRecipients;
    }
}
