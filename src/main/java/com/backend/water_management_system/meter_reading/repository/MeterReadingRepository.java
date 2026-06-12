package com.backend.water_management_system.meter_reading.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.meter_reading.entity.MeterReading;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    
    // Existing method
    List<MeterReading> findByReadingDate(LocalDate readingDate);

    // 1. Fix for: findAllByYear(int)
    @Query("SELECT m FROM MeterReading m WHERE YEAR(m.readingDate) = :year")
    List<MeterReading> findAllByYear(@Param("year") int year);

    // 2. Fix for: findByCustomerAndYear(String, int)
    
    @Query("SELECT m FROM MeterReading m WHERE m.customer.subscriptionNumber = :subNum AND YEAR(m.readingDate) = :year")
    List<MeterReading> findByCustomerAndYear(@Param("subNum") String subNum, @Param("year") int year);
   
}