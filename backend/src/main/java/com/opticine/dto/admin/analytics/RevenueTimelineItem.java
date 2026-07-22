package com.opticine.dto.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTimelineItem {
    private LocalDate date;
    private BigDecimal revenue;
    private BigDecimal actualPaidRevenue;
    private Long paidBookings;
    private Long totalBookings;
    private Long ticketsSold;
}
