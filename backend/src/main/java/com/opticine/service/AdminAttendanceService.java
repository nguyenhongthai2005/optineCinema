package com.opticine.service;

import com.opticine.dto.admin.attendance.AttendanceRequest;
import com.opticine.dto.admin.attendance.AttendanceResponse;
import com.opticine.dto.admin.attendance.AttendanceSummaryResponse;
import com.opticine.entity.Attendance;
import com.opticine.entity.Role;
import com.opticine.entity.User;
import com.opticine.repository.AttendanceRepository;
import com.opticine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@Service
public class AdminAttendanceService {
    private static final LocalTime LATE_AFTER = LocalTime.of(9, 0);

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public AdminAttendanceService(AttendanceRepository attendanceRepository, UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    public List<AttendanceResponse> search(Long staffId, LocalDate fromDate, LocalDate toDate, String status) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        return attendanceRepository.search(staffId, fromDate, toDate, normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    public AttendanceSummaryResponse monthlySummary(Integer month, Integer year, Long staffId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<Attendance> records = attendanceRepository.search(staffId, yearMonth.atDay(1), yearMonth.atEndOfMonth(), null);
        return AttendanceSummaryResponse.builder()
                .staffId(staffId)
                .month(month)
                .year(year)
                .present(countStatuses(records, "PRESENT", "CHECKED_IN", "COMPLETED", "EARLY_LEAVE", "MANUAL_ADJUSTED"))
                .absent(countStatus(records, "ABSENT"))
                .late(countStatus(records, "LATE"))
                .leaveDays(countStatus(records, "LEAVE"))
                .totalRecords((long) records.size())
                .build();
    }

    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        validateTimes(request);
        User staff = findStaff(request.getStaffId());
        if (attendanceRepository.existsByStaffIdAndBusinessDate(staff.getId(), request.getWorkDate())) {
            throw new IllegalArgumentException("Attendance already exists for this staff and date");
        }
        Attendance attendance = new Attendance();
        attendance.setStaff(staff);
        applyRequest(attendance, request);
        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request) {
        validateTimes(request);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found"));
        User staff = findStaff(request.getStaffId());
        if (attendanceRepository.existsByStaffIdAndBusinessDateAndIdNot(staff.getId(), request.getWorkDate(), id)) {
            throw new IllegalArgumentException("Attendance already exists for this staff and date");
        }
        attendance.setStaff(staff);
        applyRequest(attendance, request);
        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new IllegalArgumentException("Attendance not found");
        }
        attendanceRepository.deleteById(id);
    }

    private User findStaff(Long staffId) {
        User staff = userRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Staff not found"));
        boolean customer = staff.getRoles() != null && staff.getRoles().stream().map(Role::getName).anyMatch("ROLE_CUSTOMER"::equals);
        if (customer) {
            throw new IllegalArgumentException("User is not staff");
        }
        return staff;
    }

    private void validateTimes(AttendanceRequest request) {
        if (request.getCheckInTime() != null && request.getCheckOutTime() != null
                && !request.getCheckOutTime().isAfter(request.getCheckInTime())) {
            throw new IllegalArgumentException("Check-out time must be after check-in time");
        }
    }

    private void applyRequest(Attendance attendance, AttendanceRequest request) {
        attendance.setBusinessDate(request.getWorkDate());
        attendance.setCheckInTime(request.getCheckInTime());
        attendance.setCheckOutTime(request.getCheckOutTime());
        attendance.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase(Locale.ROOT) : "PRESENT");
        attendance.setNote(request.getNote());
        attendance.setLateMinutes(calculateLateMinutes(request));
        attendance.setWorkedMinutes(calculateWorkedMinutes(request));
        attendance.setAutoMarkedAbsent("ABSENT".equalsIgnoreCase(attendance.getStatus()));
    }

    private Integer calculateLateMinutes(AttendanceRequest request) {
        if (request.getCheckInTime() == null || request.getCheckInTime().toLocalTime().isBefore(LATE_AFTER)) {
            return 0;
        }
        return (int) Duration.between(LATE_AFTER, request.getCheckInTime().toLocalTime()).toMinutes();
    }

    private Integer calculateWorkedMinutes(AttendanceRequest request) {
        if (request.getCheckInTime() == null || request.getCheckOutTime() == null) {
            return 0;
        }
        return (int) Duration.between(request.getCheckInTime(), request.getCheckOutTime()).toMinutes();
    }

    private Long countStatus(List<Attendance> records, String status) {
        return records.stream().filter(record -> status.equalsIgnoreCase(record.getStatus())).count();
    }

    private Long countStatuses(List<Attendance> records, String... statuses) {
        return records.stream()
                .filter(record -> record.getStatus() != null
                        && java.util.Arrays.stream(statuses).anyMatch(status -> status.equalsIgnoreCase(record.getStatus())))
                .count();
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .staffId(attendance.getStaff().getId())
                .staffName(attendance.getStaff().getFullName())
                .workDate(attendance.getBusinessDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .lateMinutes(attendance.getLateMinutes())
                .workedMinutes(attendance.getWorkedMinutes())
                .earlyLeaveMinutes(attendance.getEarlyLeaveMinutes())
                .assignmentId(attendance.getAssignmentId())
                .shiftName(attendance.getShiftName())
                .shiftStartTime(attendance.getShiftStartTime())
                .shiftEndTime(attendance.getShiftEndTime())
                .checkInLatitude(attendance.getCheckInLat())
                .checkInLongitude(attendance.getCheckInLng())
                .checkInAccuracyMeters(attendance.getCheckInAccuracyMeters())
                .checkInDistanceMeters(attendance.getCheckInDistanceMeters())
                .checkInLocationValid(attendance.getCheckInLocationValid())
                .checkOutLatitude(attendance.getCheckOutLat())
                .checkOutLongitude(attendance.getCheckOutLng())
                .checkOutAccuracyMeters(attendance.getCheckOutAccuracyMeters())
                .checkOutDistanceMeters(attendance.getCheckOutDistanceMeters())
                .checkOutLocationValid(attendance.getCheckOutLocationValid())
                .workplaceName(attendance.getWorkplaceName())
                .workplaceLatitude(attendance.getWorkplaceLatitude())
                .workplaceLongitude(attendance.getWorkplaceLongitude())
                .allowedRadiusMeters(attendance.getAllowedRadiusMeters())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
