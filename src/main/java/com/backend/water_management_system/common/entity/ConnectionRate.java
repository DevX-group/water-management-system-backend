package com.backend.water_management_system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "connection_rates")
@Data
public class ConnectionRate {

    @Id
    @Column(name = "connection_type")
    private String connectionType; // 'metered' or 'non_metered'

    private Double baseRate;
    private Double unitRateTier1;
    private Double unitRateTier2;
    private Double unitRateTier3;
    private Integer tier1Limit;
    private Integer tier2Limit;
    private Double taxRate;
}