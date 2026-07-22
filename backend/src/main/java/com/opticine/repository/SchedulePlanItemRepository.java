package com.opticine.repository;

import com.opticine.entity.SchedulePlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulePlanItemRepository extends JpaRepository<SchedulePlanItem, Long> {
    List<SchedulePlanItem> findByPlanIdOrderByStartTimeAsc(Long planId);
}
