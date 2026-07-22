package com.opticine.service;

import com.opticine.dto.payment.VietQrPaymentInfoResponse;
import com.opticine.entity.Booking;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.entity.Room;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.entity.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VietQrService {
    private final ComboService comboService;

    @Value("${app.payment.vietqr.enabled:true}")
    private boolean enabled;

    @Value("${app.payment.vietqr.auto-confirm-enabled:true}")
    private boolean autoConfirmEnabled;

    @Value("${app.payment.vietqr.demo-amount-enabled:true}")
    private boolean demoAmountEnabled;

    @Value("${app.payment.vietqr.demo-amount-rate:0.1}")
    private BigDecimal demoAmountRate;

    @Value("${app.payment.vietqr.demo-round-to:1000}")
    private BigDecimal demoRoundTo;

    @Value("${app.payment.vietqr.demo-min-amount:1000}")
    private BigDecimal demoMinAmount;

    @Value("${app.payment.vietqr.bank-id:MB}")
    private String bankId;

    @Value("${app.payment.vietqr.account-no:}")
    private String accountNo;

    @Value("${app.payment.vietqr.account-name:}")
    private String accountName;

    @Value("${app.payment.vietqr.template:compact2}")
    private String template;

    public VietQrPaymentInfoResponse buildPaymentInfo(Booking booking) {
        if (!enabled) {
            throw new ConflictException("Thanh toán VietQR đang tạm ngừng.");
        }
        if (accountNo == null || accountNo.isBlank()) {
            throw new ConflictException("Tài khoản nhận tiền VietQR chưa được cấu hình.");
        }

        BigDecimal finalAmount = calculateBookingTotal(booking);
        BigDecimal payableAmount = calculateVietQrPayableAmount(finalAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền thanh toán phải lớn hơn 0.");
        }

        String transferContent = booking.getPaymentReference();
        if (transferContent == null || transferContent.isBlank()) {
            transferContent = generateTransferContent(booking.getId());
        }

        String qrImageUrl = UriComponentsBuilder
                .fromHttpUrl("https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-" + template + ".png")
                .queryParam("amount", payableAmount.longValue())
                .queryParam("addInfo", transferContent)
                .queryParam("accountName", accountName == null ? "" : accountName)
                .build()
                .encode()
                .toUriString();

        return VietQrPaymentInfoResponse.builder()
                .bookingId(booking.getId())
                .amount(payableAmount)
                .originalAmount(finalAmount)
                .grossAmount(grossAmount(booking))
                .voucherDiscountAmount(booking.getVoucherDiscountAmount() != null ? booking.getVoucherDiscountAmount() : BigDecimal.ZERO)
                .membershipTierName(booking.getMembershipTierName())
                .membershipDiscountPercent(booking.getMembershipDiscountPercent() != null ? booking.getMembershipDiscountPercent() : BigDecimal.ZERO)
                .membershipDiscountAmount(booking.getMembershipDiscountAmount() != null ? booking.getMembershipDiscountAmount() : BigDecimal.ZERO)
                .discountAmount(booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO)
                .finalAmount(finalAmount)
                .payableAmount(payableAmount)
                .demoDiscountAmount(finalAmount.subtract(payableAmount).max(BigDecimal.ZERO))
                .demoMode(demoAmountEnabled)
                .autoConfirmEnabled(autoConfirmEnabled)
                .transferContent(transferContent)
                .bankId(bankId)
                .accountNo(accountNo)
                .accountName(accountName)
                .qrImageUrl(qrImageUrl)
                .build();
    }

    public String generateTransferContent(Long bookingId) {
        String raw = "OPTI" + bookingId;
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        return normalized.length() > 18 ? normalized.substring(0, 18) : normalized;
    }

    public BigDecimal calculateBookingTotal(Booking booking) {
        if (booking.getFinalAmount() != null) {
            return booking.getFinalAmount();
        }
        BigDecimal ticketTotal = booking.getSeats().stream()
                .map(sts -> calculateSeatPrice(booking, sts))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comboTotal = booking.getId() != null
                ? comboService.comboTotal(booking.getId())
                : comboService.comboTotal(booking.getComboItems());
        BigDecimal discountAmount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        return ticketTotal.add(comboTotal).subtract(discountAmount).max(BigDecimal.ZERO);
    }

    public BigDecimal grossAmount(Booking booking) {
        if (booking.getGrossTotal() != null) {
            return booking.getGrossTotal();
        }
        BigDecimal ticketTotal = booking.getTicketTotal() != null ? booking.getTicketTotal() : booking.getSeats().stream()
                .map(sts -> calculateSeatPrice(booking, sts))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comboTotal = booking.getComboTotal() != null ? booking.getComboTotal() : booking.getId() != null
                ? comboService.comboTotal(booking.getId())
                : comboService.comboTotal(booking.getComboItems());
        return ticketTotal.add(comboTotal);
    }

    public BigDecimal calculateVietQrPayableAmount(BigDecimal originalAmount) {
        if (originalAmount == null) return BigDecimal.ZERO;
        if (!demoAmountEnabled) return originalAmount;
        BigDecimal rate = demoAmountRate != null ? demoAmountRate : BigDecimal.ONE;
        BigDecimal roundTo = demoRoundTo != null && demoRoundTo.compareTo(BigDecimal.ZERO) > 0 ? demoRoundTo : BigDecimal.ONE;
        BigDecimal minAmount = demoMinAmount != null ? demoMinAmount : BigDecimal.ZERO;
        BigDecimal raw = originalAmount.multiply(rate);
        BigDecimal rounded = raw.divide(roundTo, 0, RoundingMode.CEILING).multiply(roundTo);
        return rounded.max(minAmount);
    }

    public boolean isAutoConfirmEnabled() {
        return autoConfirmEnabled;
    }

    public boolean isDemoAmountEnabled() {
        return demoAmountEnabled;
    }

    public BigDecimal calculateSeatPrice(Booking booking, ShowtimeSeat sts) {
        BigDecimal base = sts.getSeat() != null && sts.getSeat().getBasePrice() != null
                ? sts.getSeat().getBasePrice()
                : BigDecimal.ZERO;
        Room room = booking.getShowtime() != null ? booking.getShowtime().getRoom() : null;
        TimeSlot timeSlot = booking.getShowtime() != null ? booking.getShowtime().getTimeSlot() : null;
        BigDecimal roomMulti = room != null && room.getPriceMultiplier() != null ? room.getPriceMultiplier() : BigDecimal.ONE;
        BigDecimal timeMulti = timeSlot != null && timeSlot.getPriceMultiplier() != null ? timeSlot.getPriceMultiplier() : BigDecimal.ONE;
        return base.multiply(roomMulti).multiply(timeMulti);
    }
}
