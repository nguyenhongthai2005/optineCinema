package com.opticine.dto.promotion;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PromotionValidateResponse {
    private boolean valid;
    private String message;
    private String code;
    private String discountType;       // PERCENT | FIXED
    private BigDecimal discountValue;
    private BigDecimal discountAmount; // Số tiền thực tế được giảm
    private BigDecimal maxDiscountAmount;
    private String applicableTo;       // ALL | TICKET | COMBO
}
