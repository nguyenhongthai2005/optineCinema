package com.opticine.dto.admin.attendance;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResponse {
    private Long id;
    private Long staffId;
    private String staffName;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;
    private String note;
    private Integer lateMinutes;
    private Integer workedMinutes;
    private Integer earlyLeaveMinutes;
    private Long assignmentId;
    private String shiftName;
    private String shiftStartTime;
    private String shiftEndTime;
    private BigDecimal checkInLatitude;
    private BigDecimal checkInLongitude;
    private BigDecimal checkInAccuracyMeters;
    private BigDecimal checkInDistanceMeters;
    private Boolean checkInLocationValid;
    private BigDecimal checkOutLatitude;
    private BigDecimal checkOutLongitude;
    private BigDecimal checkOutAccuracyMeters;
    private BigDecimal checkOutDistanceMeters;
    private Boolean checkOutLocationValid;
    private String workplaceName;
    private BigDecimal workplaceLatitude;
    private BigDecimal workplaceLongitude;
    private Integer allowedRadiusMeters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
