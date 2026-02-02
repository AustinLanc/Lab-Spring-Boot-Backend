package org.example.companyboiler.repository;

import org.example.companyboiler.model.QcLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QcLogRepository extends JpaRepository<QcLog, String> {

}
