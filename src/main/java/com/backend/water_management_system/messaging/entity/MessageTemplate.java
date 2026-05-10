package com.backend.water_management_system.messaging.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message_templates")
@NoArgsConstructor
@Getter
@Setter
public class MessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_custom")
    private boolean isCustom;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String subject; // used for email templates

    @OneToMany(mappedBy = "messageTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sectionOrder ASC")
    private List<TemplateSection> sections = new ArrayList<>();
}
