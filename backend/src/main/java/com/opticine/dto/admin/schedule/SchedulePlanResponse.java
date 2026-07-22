package com.opticine.dto.admin.schedule;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SchedulePlanResponse {
    private Long planId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String mode;
    private String status;
    private BigDecimal estimatedRevenue;
    private Integer estimatedTickets;
    private BigDecimal roomUtilization;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<SchedulePlanItemResponse> items;
}
