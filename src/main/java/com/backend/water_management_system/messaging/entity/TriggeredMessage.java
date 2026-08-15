package com.backend.water_management_system.messaging.entity;

import com.backend.water_management_system.messaging.enums.TriggerType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "triggered_messages")
@NoArgsConstructor
@Getter
@Setter
public class TriggeredMessage extends Message {

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType;

    @Column(name = "active")
    private boolean active;
}
