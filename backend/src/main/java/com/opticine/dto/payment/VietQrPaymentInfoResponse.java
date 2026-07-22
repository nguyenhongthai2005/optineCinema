package com.opticine.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VietQrPaymentInfoResponse {
    private Long bookingId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal grossAmount;
    private BigDecimal voucherDiscountAmount;
    private String membershipTierName;
    private BigDecimal membershipDiscountPercent;
    private BigDecimal membershipDiscountAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal payableAmount;
    private BigDecimal demoDiscountAmount;
    private boolean demoMode;
    private boolean autoConfirmEnabled;
    private String transferContent;
    private String bankId;
    private String accountNo;
    private String accountName;
    private String qrImageUrl;
}
