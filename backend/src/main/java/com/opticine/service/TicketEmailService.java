package com.opticine.service;

import com.opticine.entity.Booking;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.entity.Ticket;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEmailService {

    private final JavaMailSender mailSender;
    private final QrCodeService qrCodeService;
    private final ComboService comboService;
    private final VietQrService vietQrService;

    @Value("${app.mail-from}")
    private String mailFrom;

    public void sendTicketEmail(Booking booking, List<Ticket> tickets) {
        if (booking.getCustomer() == null || booking.getCustomer().getEmail() == null || booking.getCustomer().getEmail().isBlank()) {
            throw new RuntimeException("Customer email is empty");
        }
        if (tickets == null || tickets.isEmpty()) {
            throw new RuntimeException("No tickets to send");
        }

        try {
            log.info("Sending ticket email for booking {} to {} with {} ticket(s)",
                    booking.getId(), booking.getCustomer().getEmail(), tickets.size());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );
            helper.setFrom(mailFrom);
            helper.setTo(booking.getCustomer().getEmail());
            helper.setSubject("Xác nhận đặt vé thành công - Opticine #" + booking.getId());
            helper.setText(buildHtml(booking, tickets), true);

            int generatedQrImages = 0;
            for (Ticket ticket : tickets) {
                String contentId = qrContentId(ticket);
                byte[] qrPng = qrCodeService.generatePng(ticket.getQrCode(), 260, 260);
                helper.addInline(contentId, new ByteArrayResource(qrPng), "image/png");
                helper.addAttachment("ticket-" + ticket.getId() + "-qr.png", new ByteArrayResource(qrPng), "image/png");
                generatedQrImages++;
            }
            log.info("Generated {} inline QR image(s) and attachment fallback(s) for booking {}",
                    generatedQrImages, booking.getId());

            mailSender.send(message);
            log.info("Ticket email sent for booking {} to {}", booking.getId(), booking.getCustomer().getEmail());
        } catch (Exception e) {
            throw new RuntimeException("Cannot send ticket email", e);
        }
    }

    private String buildHtml(Booking booking, List<Ticket> tickets) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String movieTitle = booking.getShowtime() != null && booking.getShowtime().getMovie() != null
                ? booking.getShowtime().getMovie().getTitle()
                : "Đang cập nhật";
        String roomName = booking.getShowtime() != null && booking.getShowtime().getRoom() != null
                ? booking.getShowtime().getRoom().getName()
                : "Phòng chưa xác định";
        String showtime = booking.getShowtime() != null && booking.getShowtime().getStartTime() != null
                ? booking.getShowtime().getStartTime().format(formatter)
                : "Đang cập nhật";
        BigDecimal ticketTotal = valueOrZero(booking.getTicketTotal());
        var comboItems = comboService.bookingComboResponses(booking.getId());
        BigDecimal comboTotal = valueOrZero(booking.getComboTotal());
        BigDecimal grossTotal = valueOrZero(booking.getGrossTotal()).compareTo(BigDecimal.ZERO) > 0
                ? booking.getGrossTotal()
                : ticketTotal.add(comboTotal);
        BigDecimal discountAmount = valueOrZero(booking.getDiscountAmount());
        BigDecimal voucherDiscountAmount = booking.getVoucherDiscountAmount() != null
                ? booking.getVoucherDiscountAmount()
                : booking.getPromotionCode() != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal membershipDiscountAmount = valueOrZero(booking.getMembershipDiscountAmount());
        BigDecimal membershipDiscountPercent = valueOrZero(booking.getMembershipDiscountPercent());
        BigDecimal finalAmount = valueOrZero(booking.getFinalAmount()).compareTo(BigDecimal.ZERO) > 0
                ? booking.getFinalAmount()
                : grossTotal.subtract(discountAmount).max(BigDecimal.ZERO);

        StringBuilder html = new StringBuilder();
        html.append("""
                <div style="font-family:Arial,sans-serif;background:#0f172a;color:#e2e8f0;padding:24px">
                  <div style="max-width:720px;margin:0 auto;background:#111827;border-radius:12px;overflow:hidden;border:1px solid #334155">
                    <div style="background:#e11d48;color:white;padding:20px;text-align:center">
                      <h1 style="margin:0">Vé xem phim Opticine</h1>
                    </div>
                    <div style="padding:24px">
                """);
        html.append("<p>Xin chào <strong>").append(escape(booking.getCustomer().getFullName())).append("</strong>,</p>");
        html.append("<p>Cảm ơn bạn đã đặt vé tại Opticine. Vé của bạn đã được xác nhận.</p>");
        html.append("<h2 style=\"color:#f8fafc\">Đơn đặt vé #").append(booking.getId()).append("</h2>");
        html.append("<p><strong>Phim:</strong> ").append(escape(movieTitle)).append("</p>");
        html.append("<p><strong>Suất chiếu:</strong> ").append(showtime).append("</p>");
        html.append("<p><strong>Phòng:</strong> ").append(escape(roomName)).append("</p>");
        html.append("<h3 style=\"margin-top:20px\">Chi tiết thanh toán</h3>");
        html.append("<p><strong>Tiền vé:</strong> ").append(vnd(ticketTotal)).append("</p>");
        if (!comboItems.isEmpty()) {
            html.append("<h3 style=\"margin-top:20px\">Bắp nước / combo</h3>");
            html.append("<ul style=\"padding-left:18px\">");
            for (var item : comboItems) {
                html.append("<li>")
                        .append(escape(item.getComboName()))
                        .append(" x ")
                        .append(item.getQuantity())
                        .append(" - ")
                        .append(vnd(item.getSubtotal()))
                        .append("</li>");
            }
            html.append("</ul>");
            html.append("<p><strong>Bắp nước/combo:</strong> ").append(vnd(comboTotal)).append("</p>");
        }
        html.append("<p><strong>Tạm tính:</strong> ").append(vnd(grossTotal)).append("</p>");
        if (booking.getPromotionCode() != null && voucherDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Voucher ").append(escape(booking.getPromotionCode())).append(":</strong> -")
                    .append(vnd(voucherDiscountAmount)).append("</p>");
        }
        if (membershipDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Ưu đãi thành viên ")
                    .append(escape(booking.getMembershipTierName()))
                    .append(" (")
                    .append(percent(membershipDiscountPercent))
                    .append("%):</strong> -")
                    .append(vnd(membershipDiscountAmount))
                    .append("</p>");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Tổng giảm giá:</strong> -").append(vnd(discountAmount)).append("</p>");
        }
        html.append("<p><strong>Tổng thanh toán:</strong> ").append(vnd(finalAmount)).append("</p>");
        if ("VIETQR".equals(booking.getPaymentMethod()) && vietQrService.isDemoAmountEnabled()) {
            BigDecimal payable = vietQrService.calculateVietQrPayableAmount(finalAmount);
            html.append("<p><strong>Số tiền chuyển khoản demo:</strong> ")
                    .append(vnd(payable))
                    .append("</p>");
        }
        html.append("<p><strong>Trạng thái thanh toán:</strong> Đã thanh toán</p>");

        html.append("<h3 style=\"margin-top:28px\">Danh sách vé</h3>");
        for (Ticket ticket : tickets) {
            ShowtimeSeat seat = ticket.getShowtimeSeat();
            String seatLabel = seat != null && seat.getSeat() != null
                    ? seat.getSeat().getRowLabel() + seat.getSeat().getColumnNumber()
                    : "N/A";
            html.append("<div style=\"background:#1f2937;border-radius:10px;padding:16px;margin:14px 0;border:1px solid #334155\">");
            html.append("<p><strong>Vé #").append(ticket.getId()).append("</strong></p>");
            html.append("<p><strong>Ghế:</strong> ").append(escape(seatLabel)).append("</p>");
            html.append("<p><strong>Giá vé:</strong> ").append(vnd(ticket.getPrice())).append("</p>");
            html.append("<img src=\"cid:").append(qrContentId(ticket)).append("\" alt=\"Mã QR vé\" style=\"width:180px;height:180px;background:white;padding:8px;border-radius:8px;display:block\"/>");
            html.append("<p style=\"font-family:monospace;word-break:break-all;color:#cbd5e1\"><strong>QR Code:</strong> ")
                    .append(escape(ticket.getQrCode()))
                    .append("</p>");
            html.append("</div>");
        }

        html.append("""
                    <p style="color:#94a3b8">Vui lòng đưa mã QR này cho nhân viên soát vé tại rạp.</p>
                    </div>
                  </div>
                </div>
                """);
        return html.toString();
    }

    private String qrContentId(Ticket ticket) {
        return "ticket-qr-" + ticket.getId();
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String vnd(BigDecimal value) {
        return String.format("%,.0fđ", valueOrZero(value)).replace(",", ".");
    }

    private String percent(BigDecimal value) {
        return valueOrZero(value).stripTrailingZeros().toPlainString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
