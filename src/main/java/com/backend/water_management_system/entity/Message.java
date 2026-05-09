package com.backend.water_management_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NoArgsConstructor
@Getter
@Setter
public abstract class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @Column(name = "channels")
    private String channels;

    private String recipients;

    @Column(name = "is_default")
    private boolean isDefault;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sms_template_id")
    private MessageTemplate smsTemplate;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "email_template_id")
    private MessageTemplate emailTemplate;

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
