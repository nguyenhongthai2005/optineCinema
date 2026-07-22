package com.opticine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(length = 50)
    private String status;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "worked_minutes")
    private Integer workedMinutes;

    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes;

    @Column(name = "shift_name", length = 100)
    private String shiftName;

    @Column(name = "shift_start_time", length = 10)
    private String shiftStartTime;

    @Column(name = "shift_end_time", length = 10)
    private String shiftEndTime;

    @Column(name = "check_in_lat", precision = 10, scale = 8)
    private BigDecimal checkInLat;

    @Column(name = "check_in_lng", precision = 11, scale = 8)
    private BigDecimal checkInLng;

    @Column(name = "check_in_accuracy_meters", precision = 10, scale = 2)
    private BigDecimal checkInAccuracyMeters;

    @Column(name = "check_in_distance_meters", precision = 10, scale = 2)
    private BigDecimal checkInDistanceMeters;

    @Column(name = "check_in_location_valid")
    private Boolean checkInLocationValid;

    @Column(name = "check_in_ip_address", length = 80)
    private String checkInIpAddress;

    @Column(name = "check_in_user_agent", length = 500)
    private String checkInUserAgent;

    @Column(name = "check_out_lat", precision = 10, scale = 8)
    private BigDecimal checkOutLat;

    @Column(name = "check_out_lng", precision = 11, scale = 8)
    private BigDecimal checkOutLng;

    @Column(name = "check_out_accuracy_meters", precision = 10, scale = 2)
    private BigDecimal checkOutAccuracyMeters;

    @Column(name = "check_out_distance_meters", precision = 10, scale = 2)
    private BigDecimal checkOutDistanceMeters;

    @Column(name = "check_out_location_valid")
    private Boolean checkOutLocationValid;

    @Column(name = "check_out_ip_address", length = 80)
    private String checkOutIpAddress;

    @Column(name = "check_out_user_agent", length = 500)
    private String checkOutUserAgent;

    @Column(name = "workplace_name", length = 150)
    private String workplaceName;

    @Column(name = "workplace_latitude", precision = 10, scale = 8)
    private BigDecimal workplaceLatitude;

    @Column(name = "workplace_longitude", precision = 11, scale = 8)
    private BigDecimal workplaceLongitude;

    @Column(name = "allowed_radius_meters")
    private Integer allowedRadiusMeters;

    @Column(name = "auto_marked_absent")
    private Boolean autoMarkedAbsent;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
