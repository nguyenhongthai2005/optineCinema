package com.opticine.dto.admin.room;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Min(1)
    @Max(26)
    private Integer totalRows;

    @Min(1)
    @Max(30)
    private Integer totalColumns;

    @Min(1)
    @Max(26)
    private Integer rowCount;

    @Min(1)
    @Max(30)
    private Integer columnCount;

    @Size(max = 50)
    private String status;

    @DecimalMin("0.1")
    @DecimalMax("10.0")
    private BigDecimal priceMultiplier;

    @Size(max = 20)
    private String screenType;
}
