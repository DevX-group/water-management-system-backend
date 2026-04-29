package com.backend.water_management_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "usage_records")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    private String area;

    private double usage;

    private double amount;

    @Column(name = "record_date")
    private LocalDate recordDate;

    // getters and setters
}