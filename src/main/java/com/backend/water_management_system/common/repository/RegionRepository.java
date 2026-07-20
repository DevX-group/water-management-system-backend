package com.backend.water_management_system.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.common.entity.Region;

public interface RegionRepository extends JpaRepository<Region, String> {
    boolean existsByRegionName(String regionName);
    
    Optional<Region> findTopByOrderByRegionCodeDesc();
}

