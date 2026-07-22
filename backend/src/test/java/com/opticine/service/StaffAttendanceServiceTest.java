package com.opticine.service;

import com.opticine.entity.User;
import com.opticine.exception.ConflictException;
import com.opticine.repository.AttendanceRepository;
import com.opticine.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAttendanceServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    AttendanceRepository attendanceRepository;

    @Mock
    StaffSchedulingService staffSchedulingService;

    @InjectMocks
    StaffService staffService;

    @Test
    void attendanceToday_withoutAssignment_disablesCheckIn() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(attendanceRepository.findByStaffIdAndBusinessDate(staff.getId(), LocalDate.now()))
                .thenReturn(Optional.empty());
        when(staffSchedulingService.findTodayAssignment(staff.getId(), LocalDate.now())).thenReturn(null);

        Map<String, Object> result = staffService.attendanceToday(staff.getId());

        assertFalse((Boolean) result.get("hasAssignment"));
        assertFalse((Boolean) result.get("canCheckIn"));
        assertEquals("Bạn chưa được phân ca hôm nay. Vui lòng liên hệ quản lý.",
                result.get("checkInBlockedReason"));
    }

    @Test
    void checkIn_withoutAssignment_isRejected() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(staffSchedulingService.findTodayAssignment(staff.getId(), LocalDate.now())).thenReturn(null);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> staffService.checkIn(staff.getId(), null, "127.0.0.1", "test"));

        assertEquals("Bạn chưa được phân ca hôm nay. Vui lòng liên hệ quản lý.", exception.getMessage());
    }

    private User staff() {
        User user = new User();
        user.setId(10L);
        user.setFullName("Nhân viên Demo");
        return user;
    }
}
