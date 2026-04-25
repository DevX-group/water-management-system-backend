package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    
    // Existing method
    List<MeterReading> findByReadingDate(LocalDate readingDate);

    // 1. Fix for: findAllByYear(int)
    @Query("SELECT m FROM MeterReading m WHERE YEAR(m.readingDate) = :year")
    List<MeterReading> findAllByYear(@Param("year") int year);

    // 2. Fix for: findByCustomerAndYear(String, int)
    // Adjust 'm.customer.subscriptionNumber' if your entity uses a different path
    @Query("SELECT m FROM MeterReading m WHERE m.customer.subscriptionNumber = :subNum AND YEAR(m.readingDate) = :year")
    List<MeterReading> findByCustomerAndYear(@Param("subNum") String subNum, @Param("year") int year);
    
    // Fallback: If your MeterReading entity doesn't have a 'customer' relationship 
    // but just a 'subscriptionNumber' string field, use this instead:
    /*
    @Query("SELECT m FROM MeterReading m WHERE m.subscriptionNumber = :subNum AND YEAR(m.readingDate) = :year")
    List<MeterReading> findByCustomerAndYear(@Param("subNum") String subNum, @Param("year") int year);
    */
}