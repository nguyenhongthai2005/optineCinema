package com.opticine.service;

import com.opticine.entity.Booking;
import com.opticine.entity.Customer;
import com.opticine.entity.Membership;
import com.opticine.entity.ShowtimeSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BookingPricingService {
    private final ComboService comboService;
    private final VietQrService vietQrService;

    public Booking ensurePricingSnapshot(Booking booking) {
        return ensurePricingSnapshot(booking, false);
    }

    public Booking ensurePricingSnapshot(Booking booking, boolean overwrite) {
        if (booking == null) return null;
        boolean missingCore = booking.getTicketTotal() == null
                || booking.getComboTotal() == null
                || booking.getGrossTotal() == null
                || booking.getDiscountAmount() == null
                || booking.getFinalAmount() == null;
        if (!overwrite && !missingCore) {
            fillMissingDiscountBreakdownDefaults(booking);
            return booking;
        }

        BigDecimal ticketTotal = calculateTicketTotal(booking);
        BigDecimal comboTotal = calculateComboTotal(booking);
        BigDecimal grossTotal = ticketTotal.add(comboTotal);
        BigDecimal voucherDiscountAmount = booking.getVoucherDiscountAmount() != null
                ? booking.getVoucherDiscountAmount()
                : booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        if (voucherDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            voucherDiscountAmount = BigDecimal.ZERO;
        }
        if (voucherDiscountAmount.compareTo(grossTotal) > 0) {
            voucherDiscountAmount = grossTotal;
        }
        applyTotals(booking, ticketTotal, comboTotal, grossTotal, voucherDiscountAmount);

        if (booking.getPromotionCode() != null && !booking.getPromotionCode().isBlank()) {
            booking.setPromotionCode(booking.getPromotionCode().trim().toUpperCase());
        }
        return booking;
    }

    public void applyPricingSnapshot(Booking booking, BigDecimal voucherDiscountAmount, String promotionCode) {
        BigDecimal normalizedDiscount = voucherDiscountAmount != null ? voucherDiscountAmount : BigDecimal.ZERO;
        booking.setVoucherDiscountAmount(normalizedDiscount);
        booking.setPromotionCode(promotionCode != null && !promotionCode.isBlank()
                ? promotionCode.trim().toUpperCase()
                : null);
        ensurePricingSnapshot(booking, true);
    }

    public BigDecimal finalAmount(Booking booking) {
        ensurePricingSnapshot(booking);
        return booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO;
    }

    public BigDecimal grossAmount(Booking booking) {
        ensurePricingSnapshot(booking);
        return booking.getGrossTotal() != null ? booking.getGrossTotal() : BigDecimal.ZERO;
    }

    public BigDecimal calculateTicketTotal(Booking booking) {
        if (booking == null || booking.getSeats() == null) return BigDecimal.ZERO;
        return booking.getSeats().stream()
                .map(seat -> calculateSeatPrice(booking, seat))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateSeatPrice(Booking booking, ShowtimeSeat seat) {
        return vietQrService.calculateSeatPrice(booking, seat);
    }

    private BigDecimal calculateComboTotal(Booking booking) {
        if (booking.getComboItems() != null && !booking.getComboItems().isEmpty()) {
            return comboService.comboTotal(booking.getComboItems());
        }
        if (booking.getId() != null) {
            return comboService.comboTotal(booking.getId());
        }
        return comboService.comboTotal(booking.getComboItems());
    }

    private void fillMissingDiscountBreakdownDefaults(Booking booking) {
        if (booking.getVoucherDiscountAmount() == null) {
            booking.setVoucherDiscountAmount(booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO);
        }
        if (booking.getMembershipDiscountPercent() == null) {
            booking.setMembershipDiscountPercent(BigDecimal.ZERO);
        }
        if (booking.getMembershipDiscountAmount() == null) {
            booking.setMembershipDiscountAmount(BigDecimal.ZERO);
        }
    }

    private void applyTotals(Booking booking, BigDecimal ticketTotal, BigDecimal comboTotal, BigDecimal grossTotal,
                             BigDecimal voucherDiscountAmount) {
        BigDecimal remainingAfterVoucher = grossTotal.subtract(voucherDiscountAmount).max(BigDecimal.ZERO);
        Membership membership = eligibleMembership(booking.getCustomer());
        BigDecimal membershipPercent = membership != null && membership.getDiscountPercent() != null
                ? membership.getDiscountPercent()
                : BigDecimal.ZERO;
        BigDecimal membershipDiscountAmount = membershipPercent.compareTo(BigDecimal.ZERO) > 0
                ? remainingAfterVoucher.multiply(membershipPercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                : BigDecimal.ZERO;
        membershipDiscountAmount = membershipDiscountAmount.min(remainingAfterVoucher).max(BigDecimal.ZERO);
        BigDecimal totalDiscount = voucherDiscountAmount.add(membershipDiscountAmount).min(grossTotal).max(BigDecimal.ZERO);

        booking.setTicketTotal(ticketTotal);
        booking.setComboTotal(comboTotal);
        booking.setGrossTotal(grossTotal);
        booking.setVoucherDiscountAmount(voucherDiscountAmount);
        booking.setMembershipTierName(membership != null ? membership.getName() : null);
        booking.setMembershipDiscountPercent(membershipPercent);
        booking.setMembershipDiscountAmount(membershipDiscountAmount);
        booking.setDiscountAmount(totalDiscount);
        booking.setFinalAmount(grossTotal.subtract(totalDiscount).max(BigDecimal.ZERO));
    }

    private Membership eligibleMembership(Customer customer) {
        if (customer == null || customer.getUser() == null || customer.getMembership() == null) {
            return null;
        }
        return customer.getMembership();
    }
}
