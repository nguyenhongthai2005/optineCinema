package com.opticine.dto.admin.seat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatGridRequest {
    @NotNull
    @Min(1)
    @Max(26)
    private Integer rowCount;

    @NotNull
    @Min(1)
    @Max(30)
    private Integer columnCount;
}
