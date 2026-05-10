package com.backend.water_management_system.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquiries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Inquiry {
    @Id
    private String id; // Use the generated ID from your frontend (e.g., INQ-123)

    private String name;
    private String email;
    private String category;
    private String status = "open"; // open, pending, resolved
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "inquiry_messages", joinColumns = @JoinColumn(name = "inquiry_id"))
    private List<InquiryMessage> messages = new ArrayList<>();
}