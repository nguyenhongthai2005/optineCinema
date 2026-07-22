package com.opticine.dto.admin.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class StaffAssignmentRequest {
    @NotNull
    private Long staffId;

    @NotBlank
    private String assignmentType;

    @NotNull
    private LocalDate workDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private Long showtimeId;
    private Long roomId;
    private String note;
}
