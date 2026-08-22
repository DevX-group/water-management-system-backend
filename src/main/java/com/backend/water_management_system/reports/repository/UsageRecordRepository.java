package com.backend.water_management_system.reports.repository;

import com.backend.water_management_system.reports.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageRecordRepository
        extends JpaRepository<UsageRecord, Long> {

    // =====================================================
    // REPORT QUERIES
    // =====================================================

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
        ORDER BY
            EXTRACT(MONTH FROM record_date),
            area
        """, nativeQuery = true)
    List<Object[]> getAreaReport(
            @Param("year") int year,
            @Param("area") String area
    );
    default List<Object[]> getAreaReport(int year) {
        return getAreaReport(year, "all");
    }

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


    // =====================================================
    // PREDICTION QUERIES
    // =====================================================

    /*
     * Monthly prediction input.
     *
     * Returns:
     * row[0] = month name
     * row[1] = month number
     * row[2] = total usage
     * row[3] = total revenue
     */
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            EXTRACT(MONTH FROM record_date) AS month_num,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_revenue
        FROM usage_records
        WHERE EXTRACT(YEAR FROM record_date) = :year
        GROUP BY
            TO_CHAR(record_date, 'Mon'),
            EXTRACT(MONTH FROM record_date)
        ORDER BY EXTRACT(MONTH FROM record_date)
        """, nativeQuery = true)
    List<Object[]> getMonthlyPredictionData(
            @Param("year") int year
    );

    /*
     * Customer prediction input.
     *
     * The database returns data only for the requested
     * customer ID and year.
     *
     * Returns:
     * row[0] = month name
     * row[1] = month number
     * row[2] = total usage
     * row[3] = total revenue
     */
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            EXTRACT(MONTH FROM record_date) AS month_num,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_revenue
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

    /*
     * Area prediction input.
     *
     * When area = "all", data for all areas is returned.
     * Otherwise, only the selected area is returned.
     *
     * Returns:
     * row[0] = month name
     * row[1] = month number
     * row[2] = area
     * row[3] = total usage
     * row[4] = total revenue
     */
    @Query(value = """
        SELECT
            TO_CHAR(record_date, 'Mon') AS month,
            EXTRACT(MONTH FROM record_date) AS month_num,
            area,
            SUM(usage) AS total_usage,
            SUM(amount) AS total_revenue
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
        ORDER BY
            EXTRACT(MONTH FROM record_date),
            area
        """, nativeQuery = true)
    List<Object[]> getAreaPredictionData(
            @Param("year") int year,
            @Param("area") String area
    );
}