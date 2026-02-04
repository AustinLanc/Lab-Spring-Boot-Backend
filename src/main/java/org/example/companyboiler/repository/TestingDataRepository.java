package org.example.companyboiler.repository;

import org.example.companyboiler.model.TestingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TestingDataRepository extends JpaRepository<TestingData, String> {

    List<TestingData> findByCode(String code);

    List<TestingData> findByDate(LocalDate date);

    @Query("SELECT t FROM TestingData t WHERE t.date >= :start AND t.date <= :end ORDER BY t.date DESC")
    List<TestingData> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM TestingData t ORDER BY t.date DESC")
    List<TestingData> findAllOrderByDateDesc();

    List<TestingData> findByBatchContainingIgnoreCase(String batch);

}
