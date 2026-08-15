package com.backend.water_management_system.messaging.entity;

import com.backend.water_management_system.messaging.enums.ScheduleType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheduled_messages")
@NoArgsConstructor
@Getter
@Setter
public class ScheduledMessage extends Message {

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type")
    private ScheduleType scheduleType;

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
}
