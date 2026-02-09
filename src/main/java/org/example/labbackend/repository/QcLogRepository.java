package org.example.labbackend.repository;

import org.example.labbackend.model.QcLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcLogRepository extends JpaRepository<QcLog, String> {

    List<QcLog> findByCode(String code);

    List<QcLog> findByReleasedBy(String releasedBy);

    List<QcLog> findByBatchContainingIgnoreCase(String batch);

    @Query("SELECT q FROM QcLog q ORDER BY q.date DESC")
    List<QcLog> findAllOrderByDateDesc();

}
