package com.opticine.controller;

import com.opticine.dto.ticket.TicketCheckInRequest;
import com.opticine.dto.ticket.TicketVerificationResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.entity.Ticket;
import com.opticine.entity.User;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.TicketRepository;
import com.opticine.repository.UserRepository;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF','MANAGER')")
public class AdminTicketController {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Value("${app.ticket.checkin-open-before-minutes:10}")
    private long checkInOpenBeforeMinutes;

    @Value("${app.ticket.checkin-close-after-minutes:30}")
    private long checkInCloseAfterMinutes;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String qrCode) {
        try {
            Ticket ticket = ticketRepository.findByQrCode(qrCode)
                    .orElseThrow(() -> new RuntimeException("QR code not found"));
            Booking booking = findBooking(ticket);
            validateTicket(ticket, booking, false);
            return ResponseEntity.ok(toResponse(ticket, booking, "Vé hợp lệ."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/check-in")
    @Transactional
    public ResponseEntity<?> checkIn(@RequestBody TicketCheckInRequest request) {
        try {
            if (request.getQrCode() == null || request.getQrCode().isBlank()) {
                throw new RuntimeException("QR code is required");
            }
            Ticket ticket = ticketRepository.findByQrCode(request.getQrCode())
                    .orElseThrow(() -> new RuntimeException("QR code not found"));
            Booking booking = findBooking(ticket);
            validateTicket(ticket, booking, true);
            validateCheckInWindow(ticket);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User staff = userRepository.findById(userDetails.getId()).orElse(null);

            ticket.setStatus("USED");
            ticket.setCheckedInAt(LocalDateTime.now());
            ticket.setCheckedInBy(staff);
            Ticket saved = ticketRepository.save(ticket);
            return ResponseEntity.ok(toResponse(saved, booking, "Check-in thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void validateTicket(Ticket ticket, Booking booking, boolean forCheckIn) {
        if (booking == null || !"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Vé chưa được thanh toán hoặc chưa được xác nhận.");
        }
        if ("USED".equals(ticket.getStatus()) || "CHECKED_IN".equals(ticket.getStatus())) {
            throw new RuntimeException("Vé đã được sử dụng.");
        }
        if ("CANCELLED".equals(ticket.getStatus())) {
            throw new RuntimeException("Vé đã bị hủy.");
        }
        if (forCheckIn && !"VALID".equals(ticket.getStatus())) {
            throw new RuntimeException("Vé chưa được thanh toán hoặc chưa được xác nhận.");
        }
    }

    private void validateCheckInWindow(Ticket ticket) {
        CheckInWindow window = checkInWindow(ticket);
        if (!window.canCheckIn()) {
            throw new RuntimeException(window.blockedReason());
        }
    }

    private Booking findBooking(Ticket ticket) {
        if (ticket.getShowtimeSeat() == null) {
            throw new RuntimeException("Ticket has no seat");
        }
        return bookingRepository.findByShowtimeSeatId(ticket.getShowtimeSeat().getId())
                .stream()
                .filter(booking -> "CONFIRMED".equals(booking.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Confirmed booking not found for ticket"));
    }

    private TicketVerificationResponse toResponse(Ticket ticket, Booking booking, String message) {
        ShowtimeSeat sts = ticket.getShowtimeSeat();
        CheckInWindow window = checkInWindow(ticket);
        String seatLabel = sts != null && sts.getSeat() != null
                ? sts.getSeat().getRowLabel() + sts.getSeat().getColumnNumber()
                : "N/A";
        return TicketVerificationResponse.builder()
                .ticketId(ticket.getId())
                .qrCode(ticket.getQrCode())
                .status(ticket.getStatus())
                .checkedInAt(ticket.getCheckedInAt())
                .checkedInBy(ticket.getCheckedInBy() != null ? ticket.getCheckedInBy().getFullName() : null)
                .bookingId(booking.getId())
                .bookingStatus(booking.getStatus())
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .customerEmail(booking.getCustomer() != null ? booking.getCustomer().getEmail() : null)
                .movieTitle(sts != null && sts.getShowtime() != null && sts.getShowtime().getMovie() != null
                        ? sts.getShowtime().getMovie().getTitle() : null)
                .startTime(sts != null && sts.getShowtime() != null ? sts.getShowtime().getStartTime() : null)
                .endTime(sts != null && sts.getShowtime() != null ? sts.getShowtime().getEndTime() : null)
                .roomName(sts != null && sts.getShowtime() != null && sts.getShowtime().getRoom() != null
                        ? sts.getShowtime().getRoom().getName() : null)
                .seatLabel(seatLabel)
                .price(ticket.getPrice())
                .canCheckIn(window.canCheckIn())
                .checkInAvailableFrom(window.availableFrom())
                .checkInClosesAt(window.closesAt())
                .blockedReason(window.blockedReason())
                .message(message)
                .build();
    }

    private CheckInWindow checkInWindow(Ticket ticket) {
        ShowtimeSeat sts = ticket.getShowtimeSeat();
        if (sts == null || sts.getShowtime() == null) {
            return new CheckInWindow(false, null, null, "Không tìm thấy suất chiếu của vé.");
        }
        LocalDateTime start = sts.getShowtime().getStartTime();
        LocalDateTime end = sts.getShowtime().getEndTime();
        LocalDateTime availableFrom = start.minusMinutes(checkInOpenBeforeMinutes);
        LocalDateTime closesAt = end.plusMinutes(checkInCloseAfterMinutes);
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        if (now.isBefore(availableFrom)) {
            return new CheckInWindow(false, availableFrom, closesAt,
                    "Chỉ được soát vé trước giờ chiếu tối đa " + checkInOpenBeforeMinutes + " phút.");
        }
        if (now.isAfter(closesAt)) {
            return new CheckInWindow(false, availableFrom, closesAt,
                    "Suất chiếu đã kết thúc, không thể soát vé.");
        }
        return new CheckInWindow(true, availableFrom, closesAt, null);
    }

    private record CheckInWindow(boolean canCheckIn, LocalDateTime availableFrom, LocalDateTime closesAt, String blockedReason) {
    }
}
