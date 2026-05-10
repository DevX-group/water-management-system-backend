package com.backend.water_management_system.messaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_sections")
@NoArgsConstructor
@Getter
@Setter
public class TemplateSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_key")
    private String sectionKey; // original frontend key (e.g. "1", "2")

    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "section_order")
    private Integer sectionOrder;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private MessageTemplate messageTemplate;
}
