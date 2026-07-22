package com.opticine.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminPendingPaymentResponse {
    private Long bookingId;
    private String customerName;
    private String customerEmail;
    private String movieTitle;
    private LocalDateTime showtimeStartTime;
    private String roomName;
    private List<String> seats;
    private BigDecimal amount;
    private String transferContent;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime createdAt;
}
