package com.opticine.dto.booking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyBookingResponse {
    private Long id;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private BigDecimal totalAmount;
    private BigDecimal ticketTotal;
    private BigDecimal comboTotal;
    private BigDecimal grossTotal;
    private BigDecimal voucherDiscountAmount;
    private String membershipTierName;
    private BigDecimal membershipDiscountPercent;
    private BigDecimal membershipDiscountAmount;
    private BigDecimal discountAmount;
    private String promotionCode;
    private BigDecimal vietQrPayableAmount;
    private BigDecimal vietQrPaidAmount;
    private BigDecimal vietQrDemoDiscountAmount;
    private Boolean vietQrDemoMode;
    private List<ComboItemResponse> comboItems;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentReference;
    private LocalDateTime createdAt;
    private int seatCount;
}
