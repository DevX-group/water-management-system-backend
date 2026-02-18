package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    List<MeterReading> findByReadingDate(LocalDate readingDate);
}
