package com.opticine.dto.admin.timeslot;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class TimeSlotResponse {
    private Long id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal priceMultiplier;
    private String status;
}
