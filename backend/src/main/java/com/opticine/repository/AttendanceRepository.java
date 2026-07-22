package com.opticine.repository;

import com.opticine.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByStaffIdAndBusinessDate(Long staffId, LocalDate businessDate);
    boolean existsByStaffIdAndBusinessDateAndIdNot(Long staffId, LocalDate businessDate, Long id);
    Optional<Attendance> findByStaffIdAndBusinessDate(Long staffId, LocalDate businessDate);

    @Query("""
            select a from Attendance a
            where (cast(:staffId as Long) is null or a.staff.id = :staffId)
            and (cast(:fromDate as date) is null or a.businessDate >= :fromDate)
            and (cast(:toDate as date) is null or a.businessDate <= :toDate)
            and (cast(:status as String) is null or upper(a.status) = upper(:status))
            order by a.businessDate desc, a.id desc
            """)
    List<Attendance> search(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") String status
    );
}
