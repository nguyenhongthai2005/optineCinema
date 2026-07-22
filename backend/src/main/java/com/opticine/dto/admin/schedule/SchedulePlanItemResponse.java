package com.opticine.dto.admin.schedule;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SchedulePlanItemResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Integer moviePopularity;
    private Long roomId;
    private String roomName;
    private String screenType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer expectedTickets;
    private BigDecimal expectedRevenue;
    private BigDecimal score;
    private String reason;
    private Long showtimeId;
}
