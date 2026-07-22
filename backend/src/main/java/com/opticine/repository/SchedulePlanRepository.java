package com.opticine.repository;

import com.opticine.entity.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulePlanRepository extends JpaRepository<SchedulePlan, Long> {
    List<SchedulePlan> findTop20ByOrderByGeneratedAtDesc();
}
