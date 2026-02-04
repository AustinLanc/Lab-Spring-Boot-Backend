package org.example.companyboiler.repository;

import org.example.companyboiler.model.Retain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RetainRepository extends JpaRepository<Retain, Long> {

    List<Retain> findByBatch(String batch);

    List<Retain> findByCode(Long code);

    List<Retain> findByBox(Long box);

    List<Retain> findByBatchContainingIgnoreCase(String batch);

    @Query("SELECT r FROM Retain r WHERE r.date >= :start AND r.date <= :end ORDER BY r.date DESC")
    List<Retain> findByDateRange(@Param("start") Date start, @Param("end") Date end);

    @Query("SELECT r FROM Retain r ORDER BY r.date DESC")
    List<Retain> findAllOrderByDateDesc();

}
