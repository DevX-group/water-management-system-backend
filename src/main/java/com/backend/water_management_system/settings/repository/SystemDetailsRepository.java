package com.backend.water_management_system.settings.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.water_management_system.settings.entity.SystemDetails;

public interface SystemDetailsRepository extends JpaRepository<SystemDetails, Long> {
    
}
