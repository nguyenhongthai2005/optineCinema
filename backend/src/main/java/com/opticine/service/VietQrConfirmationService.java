package com.opticine.service;

import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.entity.*;
import com.opticine.exception.ApiException;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.exception.ResourceNotFoundException;
import com.opticine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQrConfirmationService {
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final VietQrService vietQrService;
    private final SeatEventPublisher seatEventPublisher;
    private final TicketEmailService ticketEmailService;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;
    private final MembershipService membershipService;
    private final BookingPricingService bookingPricingService;

    @Transactional
    public Map<String, Object> confirmCustomerTransfer(Long bookingId, Long userId) {
        if (bookingId == null) {
            throw new BadRequestException("Thiếu mã đơn đặt vé.");
        }
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));
        validateOwner(booking, userId);

        if ("CONFIRMED".equals(booking.getStatus()) && "PAID".equals(booking.getPaymentStatus())) {
            Map<String, Object> response = confirmedResponse(booking, "Thanh toán VietQR đã được xác nhận tự động.");
            sendTicketEmailAfterCommit(booking, ticketRepository.findByShowtimeSeatIn(booking.getSeats()));
            return response;
        }

        if (!"VIETQR".equals(booking.getPaymentMethod())) {
            booking.setPaymentMethod("VIETQR");
        }
        if (booking.getPaymentReference() == null || booking.getPaymentReference().isBlank()) {
            booking.setPaymentReference(vietQrService.generateTransferContent(booking.getId()));
        }
        bookingPricingService.ensurePricingSnapshot(booking, true);

        if (!vietQrService.isAutoConfirmEnabled()) {
            validateConfirmable(booking);
            booking.setPaymentStatus("WAITING_CONFIRMATION");
            booking.setStatus("WAITING_CONFIRMATION");
            bookingRepository.save(booking);
            return Map.of(
                    "message", "Thanh toán của bạn đang chờ admin xác nhận.",
                    "autoConfirmEnabled", false,
                    "bookingId", booking.getId(),
                    "bookingStatus", booking.getStatus(),
                    "paymentStatus", booking.getPaymentStatus()
            );
        }

        return confirmBooking(booking, null, "Thanh toán VietQR đã được xác nhận tự động.");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> confirmBooking(Booking booking, User staff, String successMessage) {
        if ("CONFIRMED".equals(booking.getStatus()) && "PAID".equals(booking.getPaymentStatus())) {
            return confirmedResponse(booking, successMessage);
        }
        if (!"VIETQR".equals(booking.getPaymentMethod())) {
            throw new BadRequestException("Đơn đặt vé không sử dụng phương thức VietQR.");
        }
        if ("CANCELLED".equals(booking.getStatus()) || "FAILED".equals(booking.getPaymentStatus())) {
            throw new ConflictException("Thanh toán của đơn đã bị từ chối hoặc hủy.");
        }
        validateConfirmable(booking);

        bookingPricingService.ensurePricingSnapshot(booking, true);
        BigDecimal finalAmount = booking.getFinalAmount();
        BigDecimal payableAmount = vietQrService.calculateVietQrPayableAmount(finalAmount);
        LocalDateTime now = LocalDateTime.now();

        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        booking.setPaidAt(booking.getPaidAt() != null ? booking.getPaidAt() : now);
        bookingRepository.save(booking);

        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElseGet(Invoice::new);
        invoice.setBooking(booking);
        invoice.setCustomer(booking.getCustomer());
        invoice.setShowtime(booking.getShowtime());
        invoice.setTotalAmount(finalAmount);
        invoice.setStatus("PAID");
        invoice.setCustomerUsername(customerUsername(booking));
        invoice.setMembershipProcessed(invoice.getMembershipProcessed() != null ? invoice.getMembershipProcessed() : false);
        invoice.setStaff(staff);
        invoice = invoiceRepository.save(invoice);

        // Cộng điểm và cập nhật hạng thành viên.
        membershipService.processInvoice(invoice);

        Payment payment = paymentRepository.findTopByInvoiceBookingIdOrderByCreatedAtDesc(booking.getId()).orElseGet(Payment::new);
        payment.setInvoice(invoice);
        payment.setPaymentMethod("VIETQR");
        payment.setTransactionCode(booking.getPaymentReference());
        payment.setAmount(finalAmount);
        payment.setOriginalAmount(finalAmount);
        payment.setPayableAmount(payableAmount);
        payment.setPaidAmount(payableAmount);
        payment.setDemoDiscountAmount(finalAmount.subtract(payableAmount).max(BigDecimal.ZERO));
        payment.setDemoMode(vietQrService.isDemoAmountEnabled());
        payment.setStatus("PAID");
        payment.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt() : now);
        payment.setPaidAt(booking.getPaidAt());
        paymentRepository.save(payment);

        List<Ticket> tickets = createMissingTickets(booking, invoice);
        boolean changedSeats = false;
        for (ShowtimeSeat seat : booking.getSeats()) {
            if (!"BOOKED".equals(seat.getStatus())) {
                changedSeats = true;
            }
            seat.setStatus("BOOKED");
            seat.setLockedAt(null);
            seat.setLockedBy(null);
            showtimeSeatRepository.save(seat);
        }
        if (changedSeats) {
            publishSeatEvent(booking, SeatEventDto.Type.SEAT_SOLD);
        }
        sendTicketEmailAfterCommit(booking, tickets);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", successMessage);
        response.put("bookingId", booking.getId());
        response.put("bookingStatus", booking.getStatus());
        response.put("paymentStatus", booking.getPaymentStatus());
        response.put("originalAmount", finalAmount);
        response.put("grossAmount", booking.getGrossTotal());
        response.put("voucherDiscountAmount", booking.getVoucherDiscountAmount() != null ? booking.getVoucherDiscountAmount() : BigDecimal.ZERO);
        response.put("membershipTierName", booking.getMembershipTierName());
        response.put("membershipDiscountPercent", booking.getMembershipDiscountPercent() != null ? booking.getMembershipDiscountPercent() : BigDecimal.ZERO);
        response.put("membershipDiscountAmount", booking.getMembershipDiscountAmount() != null ? booking.getMembershipDiscountAmount() : BigDecimal.ZERO);
        response.put("discountAmount", booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO);
        response.put("finalAmount", finalAmount);
        response.put("payableAmount", payableAmount);
        response.put("demoMode", vietQrService.isDemoAmountEnabled());
        response.put("ticketIds", tickets.stream().map(Ticket::getId).toList());
        return response;
    }

    @Transactional
    public void expireBooking(Booking booking) {
        booking.setStatus("CANCELLED");
        booking.setPaymentStatus("EXPIRED");
        bookingRepository.save(booking);
        for (ShowtimeSeat seat : booking.getSeats()) {
            if (!"BOOKED".equals(seat.getStatus())) {
                seat.setStatus("AVAILABLE");
                seat.setLockedAt(null);
                seat.setLockedBy(null);
                showtimeSeatRepository.save(seat);
            }
        }
        publishSeatEvent(booking, SeatEventDto.Type.SEAT_RELEASED);
    }

    private void validateConfirmable(Booking booking) {
        if (!Set.of("PENDING_PAYMENT", "WAITING_PAYMENT", "WAITING_CONFIRMATION").contains(booking.getStatus())) {
            throw new ConflictException("Đơn đặt vé không còn có thể thanh toán.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (booking.getExpiredAt() != null && now.isAfter(booking.getExpiredAt())) {
            expireBooking(booking);
            throw new ConflictException("Đơn đặt vé đã hết hạn thanh toán.");
        }
        if (booking.getShowtime() != null
                && booking.getShowtime().getStartTime() != null
                && !now.isBefore(booking.getShowtime().getStartTime())) {
            expireBooking(booking);
            throw new ConflictException("Suất chiếu đã bắt đầu, không thể xác nhận thanh toán.");
        }
        showtimeAvailabilityService.requireBookableForBooking(booking.getShowtime());
        Long ownerId = booking.getCustomer() != null && booking.getCustomer().getUser() != null
                ? booking.getCustomer().getUser().getId()
                : null;
        for (ShowtimeSeat seat : booking.getSeats()) {
            if ("BOOKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế đã được đặt.");
            }
            if (!"LOCKED".equals(seat.getStatus())
                    || seat.getLockedBy() == null
                    || !seat.getLockedBy().equals(ownerId)
                    || seat.getLockedAt() == null
                    || seat.getLockedAt().plusMinutes(2).isBefore(now)) {
                expireBooking(booking);
                throw new ConflictException("Đơn đặt vé đã hết hạn thanh toán.");
            }
        }
    }

    private void validateOwner(Booking booking, Long userId) {
        boolean isOwner = booking.getCustomer() != null
                && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getId().equals(userId);
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận thanh toán cho đơn này.");
        }
    }

    private Map<String, Object> confirmedResponse(Booking booking, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        response.put("bookingId", booking.getId());
        response.put("bookingStatus", booking.getStatus());
        response.put("paymentStatus", booking.getPaymentStatus());
        bookingPricingService.ensurePricingSnapshot(booking);
        BigDecimal finalAmount = booking.getFinalAmount();
        response.put("originalAmount", finalAmount);
        response.put("grossAmount", booking.getGrossTotal());
        response.put("voucherDiscountAmount", booking.getVoucherDiscountAmount() != null ? booking.getVoucherDiscountAmount() : BigDecimal.ZERO);
        response.put("membershipTierName", booking.getMembershipTierName());
        response.put("membershipDiscountPercent", booking.getMembershipDiscountPercent() != null ? booking.getMembershipDiscountPercent() : BigDecimal.ZERO);
        response.put("membershipDiscountAmount", booking.getMembershipDiscountAmount() != null ? booking.getMembershipDiscountAmount() : BigDecimal.ZERO);
        response.put("discountAmount", booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO);
        response.put("finalAmount", finalAmount);
        response.put("payableAmount", vietQrService.calculateVietQrPayableAmount(finalAmount));
        response.put("demoMode", vietQrService.isDemoAmountEnabled());
        response.put("ticketIds", ticketRepository.findByShowtimeSeatIn(booking.getSeats()).stream().map(Ticket::getId).toList());
        return response;
    }

    private List<Ticket> createMissingTickets(Booking booking, Invoice invoice) {
        List<Ticket> existingTickets = ticketRepository.findByShowtimeSeatIn(booking.getSeats());
        Map<Long, Ticket> existingBySeatId = existingTickets.stream()
                .filter(ticket -> ticket.getShowtimeSeat() != null)
                .collect(Collectors.toMap(ticket -> ticket.getShowtimeSeat().getId(), Function.identity(), (a, b) -> a));

        List<Ticket> result = new ArrayList<>(existingTickets);
        for (ShowtimeSeat seat : booking.getSeats()) {
            if (existingBySeatId.containsKey(seat.getId())) continue;
            Ticket ticket = new Ticket();
            ticket.setInvoice(invoice);
            ticket.setShowtimeSeat(seat);
            ticket.setPrice(bookingPricingService.calculateSeatPrice(booking, seat));
            ticket.setQrCode(UUID.randomUUID().toString());
            ticket.setStatus("VALID");
            result.add(ticketRepository.save(ticket));
        }
        return result;
    }

    private void sendTicketEmailAfterCommit(Booking booking, List<Ticket> tickets) {
        if (Boolean.TRUE.equals(booking.getTicketEmailSent())) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendTicketEmailIfNeeded(booking, tickets);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendTicketEmailIfNeeded(booking, tickets);
            }
        });
    }

    private void sendTicketEmailIfNeeded(Booking booking, List<Ticket> tickets) {
        if (Boolean.TRUE.equals(booking.getTicketEmailSent())) return;
        try {
            ticketEmailService.sendTicketEmail(booking, tickets);
            booking.setTicketEmailSent(true);
            bookingRepository.save(booking);
        } catch (Exception e) {
            log.warn("Ticket email failed for VietQR booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private void publishSeatEvent(Booking booking, SeatEventDto.Type type) {
        if (booking.getShowtime() == null || booking.getSeats() == null || booking.getSeats().isEmpty()) return;
        List<Long> seatIds = booking.getSeats().stream()
                .filter(sts -> sts.getSeat() != null)
                .map(sts -> sts.getSeat().getId())
                .toList();
        seatEventPublisher.publish(booking.getShowtime().getId(), SeatEventDto.builder()
                .type(type)
                .showtimeId(booking.getShowtime().getId())
                .seatIds(seatIds)
                .byUserId(booking.getCustomer() != null && booking.getCustomer().getUser() != null
                        ? booking.getCustomer().getUser().getId()
                        : null)
                .build());
    }

    private String customerUsername(Booking booking) {
        if (booking.getCustomer() == null) return "";
        if (booking.getCustomer().getUser() != null) return booking.getCustomer().getUser().getUsername();
        if (booking.getCustomer().getPhone() != null) return booking.getCustomer().getPhone();
        return booking.getCustomer().getEmail() != null ? booking.getCustomer().getEmail() : "";
    }
}
