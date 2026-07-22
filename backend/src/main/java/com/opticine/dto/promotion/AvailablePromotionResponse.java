package com.opticine.dto.promotion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AvailablePromotionResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String typeLabel;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer remainingUses;
    private Boolean isApplicable;
    private String unavailableReason;
    private BigDecimal estimatedDiscountAmount;
    private String applicableTo;
}
