package com.opticine.dto.promotion;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PromotionValidateRequest {
    private String code;
    private BigDecimal ticketAmount;
    private BigDecimal comboAmount;
}
