package com.opticine.dto.admin.combo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminComboResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private BigDecimal price;
    private String status;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
