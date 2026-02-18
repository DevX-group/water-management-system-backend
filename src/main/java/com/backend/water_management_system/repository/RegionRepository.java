package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, String> {
}

