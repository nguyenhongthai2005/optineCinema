package com.opticine.dto.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieAnalyticsItem {
    private Long movieId;
    private String movieTitle;
    private BigDecimal revenue;
    private Long bookings;
    private Long tickets;
}
