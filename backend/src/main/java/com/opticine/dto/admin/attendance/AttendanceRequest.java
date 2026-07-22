package com.opticine.dto.admin.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRequest {
    @NotNull
    private Long staffId;

    @NotNull
    private LocalDate workDate;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    @Size(max = 50)
    private String status;

    @Size(max = 500)
    private String note;
}
