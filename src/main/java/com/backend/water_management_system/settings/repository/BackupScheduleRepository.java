package com.backend.water_management_system.settings.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.settings.entity.BackupSchedule;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {

    Optional<BackupSchedule> findFirstByOrderByIdAsc();
}
