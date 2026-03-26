package com.backend.water_management_system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;

@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Stored as comma-separated string e.g. "SMS,Email"
    @Column(name = "channels")
    private String channels;

    @Column(name = "schedule_type")
    private String scheduleType; // "Recurring" | "One-Time"

    @Column(name = "schedule_day_of_month")
    private Integer scheduleDayOfMonth;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "last_email_sent_at")
    private LocalDateTime lastEmailSentAt;

    @Column(name = "one_time_email_sent")
    private Boolean oneTimeEmailSent;

    private String recipients;

    @Column(name = "is_default")
    private boolean isDefault;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sms_template_id")
    private MessageTemplate smsTemplate;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "email_template_id")
    private MessageTemplate emailTemplate;

    // getters/setters
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

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Integer getScheduleDayOfMonth() {
        return scheduleDayOfMonth;
    }

    public void setScheduleDayOfMonth(Integer scheduleDayOfMonth) {
        this.scheduleDayOfMonth = scheduleDayOfMonth;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public LocalTime getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(LocalTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public LocalDateTime getLastEmailSentAt() {
        return lastEmailSentAt;
    }

    public void setLastEmailSentAt(LocalDateTime lastEmailSentAt) {
        this.lastEmailSentAt = lastEmailSentAt;
    }

    public Boolean getOneTimeEmailSent() {
        return oneTimeEmailSent;
    }

    public void setOneTimeEmailSent(Boolean oneTimeEmailSent) {
        this.oneTimeEmailSent = oneTimeEmailSent;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public MessageTemplate getSmsTemplate() {
        return smsTemplate;
    }

    public void setSmsTemplate(MessageTemplate smsTemplate) {
        this.smsTemplate = smsTemplate;
    }

    public MessageTemplate getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(MessageTemplate emailTemplate) {
        this.emailTemplate = emailTemplate;
    }
}
