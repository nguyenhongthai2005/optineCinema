package com.opticine.service;

import com.opticine.dto.admin.staff.StaffAssignmentRequest;
import com.opticine.dto.staff.StaffAvailabilityRequest;
import com.opticine.entity.*;
import com.opticine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffSchedulingService {
    private final UserRepository userRepository;
    private final StaffAvailabilityRepository availabilityRepository;
    private final StaffAssignmentRepository assignmentRepository;
    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;

    public List<Map<String, Object>> myAvailability(Long staffId) {
        return availabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staffId)
                .stream()
                .map(this::toAvailabilityMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createAvailability(Long staffId, StaffAvailabilityRequest request) {
        User staff = findStaff(staffId);
        validateAvailabilityTimes(request.getStartTime(), request.getEndTime());
        DayOfWeek day = parseDayOfWeek(request.getDayOfWeek());
        ensureNoAvailabilityOverlap(staffId, day, request.getStartTime(), request.getEndTime(), null);
        StaffAvailability availability = new StaffAvailability();
        availability.setStaff(staff);
        availability.setDayOfWeek(day);
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setNote(clean(request.getNote()));
        availability.setStatus("ACTIVE");
        return toAvailabilityMap(availabilityRepository.save(availability));
    }

    @Transactional
    public Map<String, Object> updateAvailability(Long staffId, Long id, StaffAvailabilityRequest request) {
        StaffAvailability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thời gian làm việc."));
        if (!availability.getStaff().getId().equals(staffId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể sửa thời gian làm việc của mình.");
        }
        validateAvailabilityTimes(request.getStartTime(), request.getEndTime());
        DayOfWeek day = parseDayOfWeek(request.getDayOfWeek());
        ensureNoAvailabilityOverlap(staffId, day, request.getStartTime(), request.getEndTime(), id);
        availability.setDayOfWeek(day);
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setNote(clean(request.getNote()));
        availability.setStatus("ACTIVE");
        return toAvailabilityMap(availabilityRepository.save(availability));
    }

    @Transactional
    public void deleteAvailability(Long staffId, Long id) {
        StaffAvailability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thời gian làm việc."));
        if (!availability.getStaff().getId().equals(staffId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể xóa thời gian làm việc của mình.");
        }
        availabilityRepository.delete(availability);
    }

    public List<Map<String, Object>> searchAssignments(LocalDate date, String position, Long staffId, String status) {
        String normalizedPosition = StringUtils.hasText(position) ? parsePosition(position).name() : null;
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        return assignmentRepository.search(date, normalizedPosition, staffId, normalizedStatus)
                .stream()
                .map(this::toAssignmentMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createAssignment(StaffAssignmentRequest request, String createdBy) {
        StaffAssignment assignment = new StaffAssignment();
        applyAssignmentRequest(assignment, request);
        assignment.setCreatedBy(createdBy);
        assignment.setStatus("SCHEDULED");
        return toAssignmentMap(assignmentRepository.save(assignment));
    }

    @Transactional
    public Map<String, Object> updateAssignment(Long id, StaffAssignmentRequest request) {
        StaffAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công."));
        applyAssignmentRequest(assignment, request);
        return toAssignmentMap(assignmentRepository.save(assignment));
    }

    @Transactional
    public Map<String, Object> cancelAssignment(Long id) {
        StaffAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công."));
        assignment.setStatus("CANCELLED");
        return toAssignmentMap(assignmentRepository.save(assignment));
    }

    public List<Map<String, Object>> suggestions(LocalDate date, String assignmentType, LocalTime startTime, LocalTime endTime) {
        WorkAssignmentType type = parseAssignmentType(assignmentType);
        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày và thời gian.");
        }
        validateAvailabilityTimes(startTime, endTime);
        return userRepository.searchStaff(null, "ACTIVE", type.getRequiredPosition().name(), null)
                .stream()
                .map(staff -> toSuggestionMap(staff, type, date, startTime, endTime))
                .toList();
    }

    public List<Map<String, Object>> myAssignments(Long staffId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from.plusDays(14);
        return assignmentRepository.findMySchedule(staffId, from, to)
                .stream()
                .map(this::toAssignmentMap)
                .toList();
    }

    public StaffAssignment findTodayAssignment(Long staffId, LocalDate date) {
        return assignmentRepository.findTopByStaffIdAndWorkDateAndStatusIgnoreCaseOrderByStartTimeAsc(staffId, date, "SCHEDULED")
                .orElse(null);
    }

    private void applyAssignmentRequest(StaffAssignment assignment, StaffAssignmentRequest request) {
        validateAvailabilityTimes(request.getStartTime(), request.getEndTime());
        User staff = findStaff(request.getStaffId());
        if (!"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
            throw new IllegalArgumentException("Không thể phân công nhân viên không hoạt động.");
        }
        WorkAssignmentType type = parseAssignmentType(request.getAssignmentType());
        StaffPosition staffPosition = parsePosition(staff.getStaffPosition());
        if (type.getRequiredPosition() != staffPosition) {
            throw new IllegalArgumentException("Loại phân công không khớp vị trí nhân viên.");
        }
        if (!isAvailable(staff.getId(), request.getWorkDate().getDayOfWeek(), request.getStartTime(), request.getEndTime())) {
            throw new IllegalArgumentException("Nhân viên không đăng ký làm việc trong khung giờ này.");
        }
        if (!assignmentRepository.findOverlapping(staff.getId(), request.getWorkDate(), request.getStartTime(), request.getEndTime(), assignment.getId()).isEmpty()) {
            throw new IllegalArgumentException("Nhân viên đã có phân công trùng thời gian.");
        }

        Showtime showtime = request.getShowtimeId() == null ? null : showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        Room room = request.getRoomId() == null ? null : roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));

        assignment.setStaff(staff);
        assignment.setAssignmentType(type);
        assignment.setWorkDate(request.getWorkDate());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setShowtime(showtime);
        assignment.setRoom(room);
        assignment.setNote(clean(request.getNote()));
        if (!StringUtils.hasText(assignment.getStatus())) {
            assignment.setStatus("SCHEDULED");
        }
    }

    private Map<String, Object> toSuggestionMap(User staff, WorkAssignmentType type, LocalDate date, LocalTime startTime, LocalTime endTime) {
        boolean available = isAvailable(staff.getId(), date.getDayOfWeek(), startTime, endTime);
        boolean assigned = !assignmentRepository.findOverlapping(staff.getId(), date, startTime, endTime, null).isEmpty();
        boolean active = "ACTIVE".equalsIgnoreCase(staff.getStatus());
        String reason = !active ? "Nhân viên không hoạt động"
                : assigned ? "Đã có phân công trùng giờ"
                : !available ? "Chưa đăng ký khung giờ này"
                : "Phù hợp";
        Map<String, Object> map = staffSummary(staff);
        map.put("assignmentType", type.name());
        map.put("availabilityMatched", available);
        map.put("alreadyAssigned", assigned);
        map.put("selectable", active && available && !assigned);
        map.put("reason", reason);
        return map;
    }

    private boolean isAvailable(Long staffId, DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.findByStaffIdAndDayOfWeekAndStatusIgnoreCase(staffId, day, "ACTIVE")
                .stream()
                .anyMatch(row -> !row.getStartTime().isAfter(startTime) && !row.getEndTime().isBefore(endTime));
    }

    private void ensureNoAvailabilityOverlap(Long staffId, DayOfWeek day, LocalTime start, LocalTime end, Long excludeId) {
        boolean overlap = availabilityRepository.findByStaffIdAndDayOfWeekAndStatusIgnoreCase(staffId, day, "ACTIVE")
                .stream()
                .filter(row -> excludeId == null || !row.getId().equals(excludeId))
                .anyMatch(row -> row.getStartTime().isBefore(end) && row.getEndTime().isAfter(start));
        if (overlap) {
            throw new IllegalArgumentException("Khung giờ làm việc bị trùng.");
        }
    }

    private void validateAvailabilityTimes(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
    }

    private User findStaff(Long staffId) {
        User staff = userRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        boolean isStaff = staff.getRoles() != null && staff.getRoles().stream().anyMatch(role -> "ROLE_STAFF".equals(role.getName()));
        if (!isStaff) {
            throw new IllegalArgumentException("Người dùng không phải nhân viên.");
        }
        return staff;
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Ngày trong tuần không hợp lệ.");
        }
    }

    private StaffPosition parsePosition(String value) {
        try {
            return StaffPosition.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Vị trí nhân viên không hợp lệ.");
        }
    }

    private WorkAssignmentType parseAssignmentType(String value) {
        try {
            return WorkAssignmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Loại phân công không hợp lệ.");
        }
    }

    private Map<String, Object> toAvailabilityMap(StaffAvailability availability) {
        Map<String, Object> map = staffSummary(availability.getStaff());
        map.put("id", availability.getId());
        map.put("dayOfWeek", availability.getDayOfWeek());
        map.put("startTime", availability.getStartTime());
        map.put("endTime", availability.getEndTime());
        map.put("note", availability.getNote());
        map.put("status", availability.getStatus());
        return map;
    }

    public Map<String, Object> toAssignmentMap(StaffAssignment assignment) {
        Map<String, Object> map = staffSummary(assignment.getStaff());
        map.put("assignmentId", assignment.getId());
        map.put("assignmentType", assignment.getAssignmentType().name());
        map.put("assignmentTypeLabel", assignment.getAssignmentType().getLabel());
        map.put("workDate", assignment.getWorkDate());
        map.put("startTime", assignment.getStartTime());
        map.put("endTime", assignment.getEndTime());
        map.put("status", assignment.getStatus());
        map.put("note", assignment.getNote());
        map.put("showtimeId", assignment.getShowtime() != null ? assignment.getShowtime().getId() : null);
        map.put("movie", assignment.getShowtime() != null && assignment.getShowtime().getMovie() != null ? assignment.getShowtime().getMovie().getTitle() : null);
        map.put("roomId", assignment.getRoom() != null ? assignment.getRoom().getId() : assignment.getShowtime() != null && assignment.getShowtime().getRoom() != null ? assignment.getShowtime().getRoom().getId() : null);
        map.put("room", assignment.getRoom() != null ? assignment.getRoom().getName() : assignment.getShowtime() != null && assignment.getShowtime().getRoom() != null ? assignment.getShowtime().getRoom().getName() : null);
        return map;
    }

    private Map<String, Object> staffSummary(User staff) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("staffId", staff.getId());
        map.put("staffName", staff.getFullName());
        map.put("username", staff.getUsername());
        map.put("position", staff.getStaffPosition());
        map.put("positionLabel", labelPosition(staff.getStaffPosition()));
        map.put("contractType", staff.getEmploymentType());
        map.put("contractTypeLabel", labelContract(staff.getEmploymentType()));
        map.put("staffStatus", staff.getStatus());
        return map;
    }

    private String labelPosition(String value) {
        try {
            return StaffPosition.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String labelContract(String value) {
        try {
            return ContractType.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
