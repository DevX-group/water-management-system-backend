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

@Entity
@Table(name = "sent_message_failures")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SentMessage getSentMessage() {
        return sentMessage;
    }

    public void setSentMessage(SentMessage sentMessage) {
        this.sentMessage = sentMessage;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public boolean isSmsFailed() {
        return smsFailed;
    }

    public void setSmsFailed(boolean smsFailed) {
        this.smsFailed = smsFailed;
    }

    public boolean isEmailFailed() {
        return emailFailed;
    }

    public void setEmailFailed(boolean emailFailed) {
        this.emailFailed = emailFailed;
    }
}
