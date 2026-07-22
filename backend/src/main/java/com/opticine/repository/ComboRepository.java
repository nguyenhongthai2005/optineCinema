package com.opticine.repository;

import com.opticine.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long>, JpaSpecificationExecutor<Combo> {
    List<Combo> findByStatusIgnoreCase(String status);
    long countByStatusIgnoreCase(String status);
}
