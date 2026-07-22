package com.opticine.dto.admin.attendance;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceSummaryResponse {
    private Long staffId;
    private Integer month;
    private Integer year;
    private Long present;
    private Long absent;
    private Long late;
    private Long leaveDays;
    private Long totalRecords;
}
