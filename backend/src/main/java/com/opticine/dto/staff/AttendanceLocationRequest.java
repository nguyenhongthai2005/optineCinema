package com.opticine.dto.staff;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AttendanceLocationRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
}
