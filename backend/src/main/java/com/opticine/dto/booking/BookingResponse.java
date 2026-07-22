package com.opticine.dto.booking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long id;
    private String status;
    private LocalDateTime expiredAt;
    private List<Long> showtimeSeatIds;
    private List<ComboItemResponse> comboItems;
    private BigDecimal ticketTotal;
    private BigDecimal comboTotal;
    private BigDecimal grossTotal;
    private BigDecimal voucherDiscountAmount;
    private String membershipTierName;
    private BigDecimal membershipDiscountPercent;
    private BigDecimal membershipDiscountAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal totalAmount;
    private String promotionCode;
}

