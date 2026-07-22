package com.opticine.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComboItemResponse {
    private Long comboId;
    private String comboName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
