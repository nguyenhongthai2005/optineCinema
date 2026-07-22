package com.opticine.dto.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingReportItem {
    private Long bookingId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long movieId;
    private String movieTitle;
    private String roomName;
    private LocalDateTime showtimeStart;
    private String seats;
    private String combos;
    private Long ticketCount;
    private BigDecimal invoiceAmount;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String bookingStatus;
    private String paymentStatus;
    private String transactionCode;
}
