package com.opticine.dto.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private BigDecimal revenue;
    private BigDecimal orderRevenue;
    private BigDecimal actualPaidRevenue;
    private BigDecimal demoDifference;
    private BigDecimal invoiceRevenue;
    private BigDecimal averageBookingValue;
    private BigDecimal previousRevenue;
    private Double revenueChangePercent;
    private Long totalBookings;
    private Long paidBookings;
    private Long pendingBookings;
    private Long cancelledBookings;
    private Long ticketsSold;
    private Double conversionRate;
    private List<RevenueTimelineItem> timeline;
    private List<MovieAnalyticsItem> topMovies;
    private List<PaymentMethodAnalyticsItem> paymentMethods;
    private List<BookingReportItem> recentBookings;
}
