package com.backend.water_management_system.reports.repository;

import com.backend.water_management_system.reports.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    //Customer Report
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

    //Area Report
    @Query(value = """
        SELECT 
            TO_CHAR(record_date, 'Mon') AS month,
            EXTRACT(MONTH FROM record_date) AS month_num,
            area,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_amount
        FROM usage_records
        WHERE EXTRACT(YEAR FROM record_date) = :year
        GROUP BY month, month_num, area
        ORDER BY month_num
    """, nativeQuery = true)
    List<Object[]> getAreaReport(@Param("year") int year);

    //Monthly Report
    @Query(value = """
    SELECT 
        TO_CHAR(record_date, 'Mon') AS month,
        SUM(usage) AS total_usage,
        SUM(amount) AS total_revenue
    FROM usage_records
    WHERE EXTRACT(YEAR FROM record_date) = :year
    GROUP BY EXTRACT(MONTH FROM record_date), TO_CHAR(record_date, 'Mon')
    ORDER BY EXTRACT(MONTH FROM record_date)
""", nativeQuery = true)
    List<Object[]> getMonthlyReport(@Param("year") int year);

    //Customer Predictions
    @Query(value = """
    SELECT 
        TO_CHAR(record_date, 'Mon') AS month,
        SUM(usage) AS total_usage
    FROM usage_records
    WHERE customer_id = :customerId
      AND EXTRACT(YEAR FROM record_date) = :year
    GROUP BY EXTRACT(MONTH FROM record_date), TO_CHAR(record_date, 'Mon')
    ORDER BY EXTRACT(MONTH FROM record_date)
""", nativeQuery = true)
    List<Object[]> getCustomerPredictionData(
            @Param("customerId") String customerId,
            @Param("year") int year
    );
}
