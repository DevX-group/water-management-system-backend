package com.backend.water_management_system.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "regions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Region {
    @Id
    private String regionCode; // e.g. "R001"

    private String regionName;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;

}
