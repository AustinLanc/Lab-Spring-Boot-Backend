package org.example.labbackend.repository;

import org.example.labbackend.model.ProductName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductNameRepository extends JpaRepository<ProductName, Integer> {

    List<ProductName> findByNameContainingIgnoreCase(String name);

}
