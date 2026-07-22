package com.opticine.dto.admin.customer;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String status;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Long totalBookings;
    private BigDecimal totalSpent;
    private LocalDateTime latestBookingDate;
    private Integer points;
}
