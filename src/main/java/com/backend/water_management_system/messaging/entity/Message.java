package com.backend.water_management_system.messaging.entity;

import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.RecipientType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_channels", joinColumns = @JoinColumn(name = "message_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private List<MessageChannel> channels = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "recipients")
    private RecipientType recipients;

    @Column(name = "is_default")
    private boolean isDefault;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sms_template_id")
    private MessageTemplate smsTemplate;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "email_template_id")
    private MessageTemplate emailTemplate;
}
