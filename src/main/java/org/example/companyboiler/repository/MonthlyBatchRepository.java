package org.example.companyboiler.repository;

import org.example.companyboiler.model.MonthlyBatch;
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

}
