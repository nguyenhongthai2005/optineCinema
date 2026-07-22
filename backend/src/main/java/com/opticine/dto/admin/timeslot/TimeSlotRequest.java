package com.opticine.dto.admin.timeslot;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class TimeSlotRequest {

    @Size(max = 50)
    private String name;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @DecimalMin("0.1")
    @DecimalMax("10.0")
    private BigDecimal priceMultiplier;

    @Size(max = 50)
    private String status;
}
