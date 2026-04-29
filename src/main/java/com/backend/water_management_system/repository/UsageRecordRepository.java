package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    @Query(value = """
        SELECT 
            TO_CHAR(record_date, 'Mon') AS month,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_amount
        FROM usage_records
        WHERE customer_id = :customerId
          AND EXTRACT(YEAR FROM record_date) = :year
        GROUP BY month, EXTRACT(MONTH FROM record_date)
        ORDER BY EXTRACT(MONTH FROM record_date)
    """, nativeQuery = true)
    List<Object[]> getCustomerReport(
            @Param("customerId") String customerId,
            @Param("year") int year
    );
}