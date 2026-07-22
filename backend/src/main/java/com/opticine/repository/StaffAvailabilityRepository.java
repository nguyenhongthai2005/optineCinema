package com.opticine.repository;

import com.opticine.entity.StaffAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailability, Long> {
    List<StaffAvailability> findByStaffIdOrderByDayOfWeekAscStartTimeAsc(Long staffId);
    List<StaffAvailability> findByStaffIdAndDayOfWeekAndStatusIgnoreCase(Long staffId, DayOfWeek dayOfWeek, String status);
    List<StaffAvailability> findByDayOfWeekAndStatusIgnoreCase(DayOfWeek dayOfWeek, String status);
}
