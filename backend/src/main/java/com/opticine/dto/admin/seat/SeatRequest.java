package com.opticine.dto.admin.seat;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SeatRequest {

    @NotBlank
    @Size(max = 5)
    private String rowLabel;

    @NotNull
    @Min(1)
    private Integer columnNumber;

    @Size(max = 20)
    private String seatType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal basePrice;

    @Size(max = 50)
    private String status;

    private Long pairedSeatId;
}
