package com.opticine.dto.showtime.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatStatusResponse {

    private Long       showtimeSeatId;   // ID của ShowtimeSeat — M3 dùng field này để booking
    private Long       seatId;
    private String     rowLabel;
    private Integer    columnNumber;
    private String     seatType;         // NORMAL | VIP | COUPLE | PREMIUM
    private String     seatTypeLabel;
    private BigDecimal basePrice;
    private BigDecimal roomMultiplier;
    private BigDecimal timeSlotMultiplier;
    private String     roomName;
    private String     screenType;
    private String     timeSlotName;
    private BigDecimal finalPrice;       // basePrice × roomMultiplier × timeSlotMultiplier
    private String     status;           // AVAILABLE | LOCKED | BOOKED
    private String     statusLabel;
    private Boolean    maintenance;
    private Boolean    lockedByCurrentUser;
    private java.time.LocalDateTime lockExpiresAt;
    private Long       pairedSeatId;     // Ghế đôi (COUPLE)
}
