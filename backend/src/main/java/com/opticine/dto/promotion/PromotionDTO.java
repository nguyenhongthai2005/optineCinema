package com.opticine.dto.promotion;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionDTO {
    private Long id;
    private String code;
    private String discountType;        // PERCENT | FIXED
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;              // ACTIVE | INACTIVE | EXPIRED
    private Integer maxUsage;
    private Integer currentUsage;
    private String applicableTo;        // ALL | TICKET | COMBO
    private BigDecimal maxDiscountAmount;
    private Integer maxUsagePerUser;
}
