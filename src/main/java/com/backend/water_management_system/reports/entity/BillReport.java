package com.backend.water_management_system.reports.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bills_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillReport {

    @Id
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    private Double amount;

    @Column(name = "billreportdue_date")
    private LocalDate dueDate;

    @Column(name = "billreport_Date")
    private LocalDate billReportDate;

    private String status;
}
