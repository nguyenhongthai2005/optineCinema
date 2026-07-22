package com.opticine.dto.ticket;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketResponse {
    private Long id;
    private String movieTitle;
    private String roomName;
    private String seatLabel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private String qrCode;
    private String status;
    private BigDecimal ticketTotal;
    private BigDecimal comboTotal;
    private BigDecimal grossTotal;
    private String promotionCode;
    private BigDecimal voucherDiscountAmount;
    private String membershipTierName;
    private BigDecimal membershipDiscountPercent;
    private BigDecimal membershipDiscountAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
}
