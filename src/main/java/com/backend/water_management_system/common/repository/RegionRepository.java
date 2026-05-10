package com.backend.water_management_system.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.common.entity.Region;

public interface RegionRepository extends JpaRepository<Region, String> {
}

