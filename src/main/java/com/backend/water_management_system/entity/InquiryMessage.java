package com.backend.water_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InquiryMessage {
    private String msgId;
    
    // Rename column to avoid PostgreSQL 'user' keyword error
    @Column(name = "sender_role") 
    private String user; 
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    private String time;

    public InquiryMessage(String user) {
        this.user = user;
    }
}