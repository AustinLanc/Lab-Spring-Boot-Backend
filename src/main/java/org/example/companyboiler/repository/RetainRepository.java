package org.example.companyboiler.repository;

import org.example.companyboiler.model.Retain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetainRepository extends JpaRepository<Retain, Long> {

}
