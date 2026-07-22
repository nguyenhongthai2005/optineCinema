package com.opticine.repository;

import com.opticine.entity.StaffAssignment;
import com.opticine.entity.WorkAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {
    @Query("""
            select a from StaffAssignment a
            where (:date is null or a.workDate = :date)
            and (:staffId is null or a.staff.id = :staffId)
            and (:position is null or a.staff.staffPosition = :position)
            and (:status is null or upper(a.status) = upper(:status))
            order by a.workDate desc, a.startTime asc
            """)
    List<StaffAssignment> search(@Param("date") LocalDate date,
                                 @Param("position") String position,
                                 @Param("staffId") Long staffId,
                                 @Param("status") String status);

    List<StaffAssignment> findByStaffIdAndWorkDateAndStatusIgnoreCase(Long staffId, LocalDate workDate, String status);

    @Query("""
            select a from StaffAssignment a
            where a.staff.id = :staffId
            and a.workDate between :fromDate and :toDate
            order by a.workDate asc, a.startTime asc
            """)
    List<StaffAssignment> findMySchedule(@Param("staffId") Long staffId,
                                         @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate);

    @Query("""
            select a from StaffAssignment a
            where a.staff.id = :staffId
            and a.workDate = :workDate
            and upper(a.status) = 'SCHEDULED'
            and a.startTime < :endTime
            and a.endTime > :startTime
            and (:excludeId is null or a.id <> :excludeId)
            """)
    List<StaffAssignment> findOverlapping(@Param("staffId") Long staffId,
                                          @Param("workDate") LocalDate workDate,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime,
                                          @Param("excludeId") Long excludeId);

    Optional<StaffAssignment> findTopByStaffIdAndWorkDateAndStatusIgnoreCaseOrderByStartTimeAsc(Long staffId, LocalDate workDate, String status);
    List<StaffAssignment> findByAssignmentTypeAndWorkDate(WorkAssignmentType assignmentType, LocalDate workDate);
    List<StaffAssignment> findByStaffIdAndWorkDateGreaterThanEqualAndStatusIgnoreCase(Long staffId, LocalDate workDate, String status);
}
