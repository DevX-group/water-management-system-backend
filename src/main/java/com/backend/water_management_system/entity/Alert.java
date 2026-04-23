package com.backend.water_management_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String severity; // critical, high, medium, info
    private String title;
    private String description;
    private String usageAmount; 
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private boolean dismissed = false;
    
    @Column(name = "subscription_number")
    private String subscriptionNumber;
}