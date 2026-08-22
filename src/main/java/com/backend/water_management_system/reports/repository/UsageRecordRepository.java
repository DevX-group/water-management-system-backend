package com.backend.water_management_system.reports.repository;

import com.backend.water_management_system.reports.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageRecordRepository
        extends JpaRepository<UsageRecord, Long> {

    // Customer report: filtered by customer ID and year
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_amount
        FROM usage_records
        WHERE customer_id = :customerId
          AND EXTRACT(YEAR FROM record_date) = :year
        GROUP BY
            TO_CHAR(record_date, 'Mon'),
            EXTRACT(MONTH FROM record_date)
        ORDER BY EXTRACT(MONTH FROM record_date)
        """, nativeQuery = true)
    List<Object[]> getCustomerReport(
            @Param("customerId") String customerId,
            @Param("year") int year
    );

    // Area report: filtered by year and selected area
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            EXTRACT(MONTH FROM record_date) AS month_num,
            area,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_amount
        FROM usage_records
        WHERE EXTRACT(YEAR FROM record_date) = :year
          AND (
              LOWER(:area) = 'all'
              OR LOWER(area) = LOWER(:area)
          )
        GROUP BY
            TO_CHAR(record_date, 'Mon'),
            EXTRACT(MONTH FROM record_date),
            area
        ORDER BY EXTRACT(MONTH FROM record_date)
        """, nativeQuery = true)
    List<Object[]> getAreaReport(
            @Param("year") int year,
            @Param("area") String area
    );

    // Monthly report: filtered by year
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_revenue
        FROM usage_records
        WHERE EXTRACT(YEAR FROM record_date) = :year
        GROUP BY
            TO_CHAR(record_date, 'Mon'),
            EXTRACT(MONTH FROM record_date)
        ORDER BY EXTRACT(MONTH FROM record_date)
        """, nativeQuery = true)
    List<Object[]> getMonthlyReport(
            @Param("year") int year
    );

    // Customer prediction data
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            SUM(usage) AS total_usage
        FROM usage_records
        WHERE customer_id = :customerId
          AND EXTRACT(YEAR FROM record_date) = :year
        GROUP BY
            TO_CHAR(record_date, 'Mon'),
            EXTRACT(MONTH FROM record_date)
        ORDER BY EXTRACT(MONTH FROM record_date)
        """, nativeQuery = true)
    List<Object[]> getCustomerPredictionData(
            @Param("customerId") String customerId,
            @Param("year") int year
    );
}