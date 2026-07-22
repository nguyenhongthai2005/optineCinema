package com.opticine.dto.admin.seat;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SeatResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private String rowLabel;
    private Integer columnNumber;
    private String seatLabel;
    private String seatType;
    private String seatTypeLabel;
    private BigDecimal basePrice;
    private String status;
    private String statusLabel;
    private Long pairedSeatId;
}
