package org.example.labbackend.repository;

import org.example.labbackend.model.MonthlyBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonthlyBatchRepository extends JpaRepository<MonthlyBatch, String> {

    List<MonthlyBatch> findByCode(Integer code);

    List<MonthlyBatch> findByType(String type);

    List<MonthlyBatch> findByReleased(String released);

    @Query("SELECT m FROM MonthlyBatch m WHERE m.dateStart >= :start AND m.dateStart <= :end")
    List<MonthlyBatch> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m FROM MonthlyBatch m ORDER BY m.dateStart DESC")
    List<MonthlyBatch> findAllOrderByDateDesc();

    @Query(value = """
        SELECT
            2020 + year_digit AS year,
            month_num AS month,
            MONTHNAME(DATE_ADD(MAKEDATE(2000, 1), INTERVAL month_num - 1 MONTH)) AS monthName,
            COUNT(CASE WHEN released = 'Yes' THEN 1 END) AS batchesReleased,
            COALESCE(SUM(CASE WHEN released = 'Yes' THEN lbs ELSE 0 END), 0) AS totalPounds,
            COUNT(CASE WHEN released = 'No' THEN 1 END) AS reworkBatches,
            COALESCE(SUM(CASE WHEN released = 'No' THEN lbs ELSE 0 END), 0) AS reworkPounds
        FROM (
            SELECT
                ASCII(SUBSTRING(batch, 2, 1)) - 64 AS month_num,
                CAST(SUBSTRING(batch, 3, 1) AS UNSIGNED) AS year_digit,
                released,
                lbs
            FROM monthly_batches
            WHERE SUBSTRING(batch, 2, 1) BETWEEN 'A' AND 'L'
        ) t
        WHERE 2020 + year_digit = :year
        GROUP BY year_digit, month_num
        ORDER BY month_num
        """, nativeQuery = true)
    List<Object[]> getMonthlyStatsByYear(@Param("year") int year);

    @Query(value = """
        SELECT DISTINCT 2020 + CAST(SUBSTRING(batch, 3, 1) AS UNSIGNED) AS year
        FROM monthly_batches
        WHERE SUBSTRING(batch, 2, 1) BETWEEN 'A' AND 'L'
        ORDER BY year DESC
        """, nativeQuery = true)
    List<Integer> getAvailableYears();

}
