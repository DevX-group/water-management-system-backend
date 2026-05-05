package com.backend.water_management_system.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "bank_slips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSlip {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slipId;
    private String publicId; // For Cloudinary public ID

    private String subscriptionNumber;
    private BigDecimal amount;
    
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private String bankReference;

    @Enumerated(EnumType.STRING)
    private SlipStatus status;

    private LocalDate bankPaymentDate; // Date on the bank slip, not when it was uploaded
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt; // When admin approves/rejects the slip
    private String rejectionReason; // Optional reason for rejection

}
