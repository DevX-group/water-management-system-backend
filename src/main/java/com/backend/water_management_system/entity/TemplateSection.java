package com.backend.water_management_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "template_sections")
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

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSectionOrder() {
        return sectionOrder;
    }

    public void setSectionOrder(Integer sectionOrder) {
        this.sectionOrder = sectionOrder;
    }
}
