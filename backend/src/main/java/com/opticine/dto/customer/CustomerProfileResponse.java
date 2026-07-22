package com.opticine.dto.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Integer points;
    private BigDecimal totalSpent;

    // Membership hiện tại
    private String membershipName;
    private BigDecimal membershipDiscount;

    // Hạng kế tiếp
    private String nextMembershipName;
    private BigDecimal nextMembershipMinSpent;

    // Tiến độ lên hạng (0-100)
    private Integer progressPercent;
}
