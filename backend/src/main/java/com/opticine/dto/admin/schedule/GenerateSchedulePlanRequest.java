package com.opticine.dto.admin.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class GenerateSchedulePlanRequest {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String mode = "MAX_REVENUE";
    private LocalTime openingTime = LocalTime.of(8, 0);
    private LocalTime closingTime = LocalTime.of(23, 30);
    private Integer cleaningMinutes = 15;
    private Boolean overwriteExisting = false;
}
