package com.opticine.controller;

import com.opticine.dto.payment.AdminPendingPaymentResponse;
import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.entity.*;
import com.opticine.repository.*;
import com.opticine.service.SeatEventPublisher;
import com.opticine.service.TicketEmailService;
import com.opticine.service.VietQrConfirmationService;
import com.opticine.service.VietQrService;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Slf4j
public class AdminPaymentController {

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;
    private final VietQrService vietQrService;
    private final SeatEventPublisher seatEventPublisher;
    private final TicketEmailService ticketEmailService;
    private final VietQrConfirmationService vietQrConfirmationService;

    @GetMapping("/pending")
    public ResponseEntity<?> pending() {
        List<AdminPendingPaymentResponse> response = bookingRepository
                .findByPaymentMethodAndPaymentStatusOrderByCreatedAtDesc("VIETQR", "WAITING_CONFIRMATION")
                .stream()
                .map(this::toPendingResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/confirm-vietqr")
    @Transactional
    public ResponseEntity<?> confirmVietQr(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            if (!"VIETQR".equals(booking.getPaymentMethod())) {
                return ResponseEntity.badRequest().body("Booking is not a VietQR payment");
            }
            if ("CANCELLED".equals(booking.getStatus()) || "FAILED".equals(booking.getPaymentStatus())) {
                return ResponseEntity.badRequest().body("Booking payment was rejected or cancelled");
            }

            return ResponseEntity.ok(vietQrConfirmationService.confirmBooking(
                    booking,
                    currentUserOrNull(),
                    "VietQR payment confirmed and tickets created."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/reject-vietqr")
    @Transactional
    public ResponseEntity<?> rejectVietQr(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            if (!"VIETQR".equals(booking.getPaymentMethod())) {
                return ResponseEntity.badRequest().body("Booking is not a VietQR payment");
            }
            if ("CONFIRMED".equals(booking.getStatus()) || "PAID".equals(booking.getPaymentStatus())) {
                return ResponseEntity.badRequest().body("Paid booking cannot be rejected");
            }

            booking.setStatus("CANCELLED");
            booking.setPaymentStatus("FAILED");
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
            return ResponseEntity.ok(Map.of("message", "VietQR payment rejected and seats released."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private List<Ticket> createMissingTickets(Booking booking, Invoice invoice) {
        List<Ticket> existingTickets = ticketRepository.findByShowtimeSeatIn(booking.getSeats());
        Map<Long, Ticket> existingBySeatId = existingTickets.stream()
                .filter(ticket -> ticket.getShowtimeSeat() != null)
                .collect(Collectors.toMap(ticket -> ticket.getShowtimeSeat().getId(), Function.identity(), (a, b) -> a));

        List<Ticket> result = new ArrayList<>(existingTickets);
        for (ShowtimeSeat seat : booking.getSeats()) {
            if (existingBySeatId.containsKey(seat.getId())) {
                continue;
            }
            Ticket ticket = new Ticket();
            ticket.setInvoice(invoice);
            ticket.setShowtimeSeat(seat);
            ticket.setPrice(vietQrService.calculateSeatPrice(booking, seat));
            ticket.setQrCode(UUID.randomUUID().toString());
            ticket.setStatus("VALID");
            result.add(ticketRepository.save(ticket));
        }
        return result;
    }

    private void sendTicketEmailIfNeeded(Booking booking, List<Ticket> tickets) {
        if (Boolean.TRUE.equals(booking.getTicketEmailSent())) {
            return;
        }
        try {
            ticketEmailService.sendTicketEmail(booking, tickets);
            booking.setTicketEmailSent(true);
            bookingRepository.save(booking);
        } catch (Exception e) {
            log.warn("Ticket email failed for VietQR booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private AdminPendingPaymentResponse toPendingResponse(Booking booking) {
        Showtime showtime = booking.getShowtime();
        Customer customer = booking.getCustomer();
        return AdminPendingPaymentResponse.builder()
                .bookingId(booking.getId())
                .customerName(customer != null ? customer.getFullName() : "")
                .customerEmail(customer != null ? customer.getEmail() : "")
                .movieTitle(showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Dang cap nhat")
                .showtimeStartTime(showtime != null ? showtime.getStartTime() : null)
                .roomName(showtime != null && showtime.getRoom() != null ? showtime.getRoom().getName() : "TBA")
                .seats(formatSeats(booking))
                .amount(vietQrService.calculateBookingTotal(booking))
                .transferContent(booking.getPaymentReference())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private List<String> formatSeats(Booking booking) {
        return booking.getSeats().stream()
                .filter(seat -> seat.getSeat() != null)
                .sorted(Comparator.comparing(seat -> seat.getSeat().getRowLabel() + seat.getSeat().getColumnNumber()))
                .map(seat -> seat.getSeat().getRowLabel() + seat.getSeat().getColumnNumber())
                .toList();
    }

    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    private void publishSeatEvent(Booking booking, SeatEventDto.Type type) {
        if (booking.getShowtime() == null || booking.getSeats() == null || booking.getSeats().isEmpty()) {
            return;
        }
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
}
