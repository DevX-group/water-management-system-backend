package com.backend.water_management_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   //automatically increment the ID (1, 2, 3...) every time a new alert is saved
    private Long id;

    private String severity;
    private String title;
    private String description;
    private String usage;
    
    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();   //This is a special Lombok instruction. It ensures that if you use the .builder() to create an alert but forget to set a time, it will automatically use the current time.

    private boolean dismissed = false;         //new alert is born, it is "Active"
}