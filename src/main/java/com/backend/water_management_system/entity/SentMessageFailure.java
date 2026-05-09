package com.backend.water_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sent_message_failures")
@NoArgsConstructor
@Getter
@Setter
public class SentMessageFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_message_id", nullable = false)
    private SentMessage sentMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_subscription_number", nullable = false)
    private Customer customer;

    @Column(name = "sms_failed", nullable = false)
    private boolean smsFailed;

    @Column(name = "email_failed", nullable = false)
    private boolean emailFailed;
}
