package com.backend.water_management_system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheduled_messages")
@NoArgsConstructor
@Getter
@Setter
public class ScheduledMessage extends Message {

    @Column(name = "schedule_type")
    private String scheduleType; // "Recurring" | "One-Time"

    @Column(name = "schedule_day_of_month")
    private Integer scheduleDayOfMonth;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "last_message_sent_at")
    private LocalDateTime lastMessageSentAt; // for recurring messages

    @Column(name = "one_time_message_sent") // for one-time messages
    private Boolean oneTimeMessageSent;

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

    public LocalDateTime getLastMessageSentAt() {
        return lastMessageSentAt;
    }

    public void setLastMessageSentAt(LocalDateTime lastMessageSentAt) {
        this.lastMessageSentAt = lastMessageSentAt;
    }

    public Boolean getOneTimeMessageSent() {
        return oneTimeMessageSent;
    }

    public void setOneTimeMessageSent(Boolean oneTimeMessageSent) {
        this.oneTimeMessageSent = oneTimeMessageSent;
    }

}
