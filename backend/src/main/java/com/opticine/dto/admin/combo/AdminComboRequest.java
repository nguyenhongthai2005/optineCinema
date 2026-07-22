package com.opticine.dto.admin.combo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminComboRequest {
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private BigDecimal price;
    private String status;
    private Integer stockQuantity;
}
