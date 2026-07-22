package com.opticine.dto.admin.room;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RoomResponse {
    private Long id;
    private String name;
    private String roomType;
    private Integer totalRows;
    private Integer totalColumns;
    private Integer rowCount;
    private Integer columnCount;
    private Integer capacity;
    private Integer totalSeats;
    private Integer seatCount;
    private String status;
    private BigDecimal priceMultiplier;
    private String screenType;
}
