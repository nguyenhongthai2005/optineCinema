package com.opticine.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketVerificationResponse {
    private Long ticketId;
    private String qrCode;
    private String status;
    private LocalDateTime checkedInAt;
    private String checkedInBy;
    private Long bookingId;
    private String bookingStatus;
    private String customerName;
    private String customerEmail;
    private String movieTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomName;
    private String seatLabel;
    private BigDecimal price;
    private Boolean canCheckIn;
    private LocalDateTime checkInAvailableFrom;
    private LocalDateTime checkInClosesAt;
    private String blockedReason;
    private String message;
}
