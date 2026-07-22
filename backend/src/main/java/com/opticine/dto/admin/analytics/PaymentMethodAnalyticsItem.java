package com.opticine.dto.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodAnalyticsItem {
    private String paymentMethod;
    private BigDecimal revenue;
    private BigDecimal actualPaidRevenue;
    private Long transactions;
}
