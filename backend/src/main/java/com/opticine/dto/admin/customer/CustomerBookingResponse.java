package com.opticine.dto.admin.customer;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CustomerBookingResponse {
    private Long id;
    private String movie;
    private LocalDateTime showtime;
    private List<String> seats;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String bookingStatus;
    private LocalDateTime createdAt;
}
