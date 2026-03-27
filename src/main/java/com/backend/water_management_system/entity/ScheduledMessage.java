package com.backend.water_management_system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;

@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage extends Message {

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

    // getters/setters

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

}
