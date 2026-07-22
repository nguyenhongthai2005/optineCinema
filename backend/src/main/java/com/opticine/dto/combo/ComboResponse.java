package com.opticine.dto.combo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComboResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private BigDecimal price;
    private String status;
    private Integer stockQuantity;
}
