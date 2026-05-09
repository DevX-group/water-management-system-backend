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
}

