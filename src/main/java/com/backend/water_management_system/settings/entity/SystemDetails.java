package com.backend.water_management_system.settings.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="system_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String companyName;
    private String officeAddress;
    private String officeContactNumber;
    private String officeEmail;
    private String defaultCurrency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
