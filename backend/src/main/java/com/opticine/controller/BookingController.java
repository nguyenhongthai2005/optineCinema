package com.opticine.controller;

import com.opticine.dto.booking.BookingRequest;
import com.opticine.dto.booking.MyBookingResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.Ticket;
import com.opticine.exception.ConflictException;
import com.opticine.exception.ResourceNotFoundException;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.TicketRepository;
import com.opticine.service.BookingService;
import com.opticine.service.StaffService;
import com.opticine.service.TicketEmailService;
import com.opticine.service.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketEmailService ticketEmailService;

    @Autowired
    private StaffService staffService;

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        staffService.denyTicketCheckerSalesAccess(userDetails);
        return ResponseEntity.ok(bookingService.createBooking(userDetails.getId(), request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<MyBookingResponse> bookings = bookingService.getMyBookings(userDetails.getId());
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/price-preview")
    public ResponseEntity<?> previewBooking(@RequestBody BookingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        staffService.denyTicketCheckerSalesAccess(userDetails);
        return ResponseEntity.ok(bookingService.previewBooking(userDetails.getId(), request));
    }

    @PostMapping("/{bookingId}/send-ticket-email")
    public ResponseEntity<?> resendTicketEmail(@PathVariable Long bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));

        boolean isOwner = booking.getCustomer() != null
                && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getId().equals(userDetails.getId());
        boolean canManage = hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_STAFF", "ROLE_MANAGER");
        if (!isOwner && !canManage) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không có quyền truy cập vé này.");
        }
        if (!"CONFIRMED".equals(booking.getStatus()) || !"PAID".equals(booking.getPaymentStatus())) {
            throw new ConflictException("Vé chưa được thanh toán hoặc chưa được xác nhận.");
        }

        List<Ticket> tickets = ticketRepository.findByShowtimeSeatIn(booking.getSeats());
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy vé của đơn đặt này.");
        }
        ticketEmailService.sendTicketEmail(booking, tickets);
        booking.setTicketEmailSent(true);
        bookingRepository.save(booking);
        return ResponseEntity.ok("Đã gửi lại email vé.");
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(roles));
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowed::contains);
    }

    @Autowired
    private com.opticine.repository.ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private com.opticine.service.SeatEventPublisher seatEventPublisher;

    @GetMapping("/reset-test-seats")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetTestSeats() {
        java.util.List<com.opticine.entity.ShowtimeSeat> seats = showtimeSeatRepository.findAll();
        for (com.opticine.entity.ShowtimeSeat seat : seats) {
            seat.setStatus("AVAILABLE");
            seat.setLockedAt(null);
            seat.setLockedBy(null);
        }
        showtimeSeatRepository.saveAll(seats);
        return ResponseEntity.ok("Đã đặt lại toàn bộ ghế về trạng thái AVAILABLE.");
    }

    @PutMapping("/{id}/cancel")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));

        if (booking.getCustomer() == null || booking.getCustomer().getUser() == null || !booking.getCustomer().getUser().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền hủy đơn đặt vé này.");
        }

        // Cho phép huỷ khi: status = PENDING_PAYMENT hoặc paymentStatus = PENDING hoặc paymentStatus = FAILED
        boolean canCancel = "PENDING_PAYMENT".equals(booking.getStatus())
                || "PENDING".equals(booking.getPaymentStatus())
                || "FAILED".equals(booking.getPaymentStatus());
        if (!canCancel) {
            throw new ConflictException("Chỉ có thể hủy đơn đặt vé đang chờ thanh toán hoặc thanh toán thất bại.");
        }

        booking.setStatus("CANCELLED");
        booking.setPaymentStatus("CANCELLED");
        bookingRepository.save(booking);

        // Free seats
        java.util.Set<com.opticine.entity.ShowtimeSeat> seats = booking.getSeats();
        seats.forEach(seat -> {
            if ("LOCKED".equals(seat.getStatus()) && userDetails.getId().equals(seat.getLockedBy())) {
                seat.setStatus("AVAILABLE");
                seat.setLockedAt(null);
                seat.setLockedBy(null);
            }
        });
        showtimeSeatRepository.saveAll(seats);

        // Broadcast seat release via WebSocket
        try {
            seatEventPublisher.publish(booking.getShowtime().getId(), com.opticine.dto.showtime.response.SeatEventDto.builder()
                    .type(com.opticine.dto.showtime.response.SeatEventDto.Type.SEAT_RELEASED)
                    .showtimeId(booking.getShowtime().getId())
                    .seatIds(seats.stream().map(s -> s.getSeat().getId()).toList())
                    .byUserId(userDetails.getId())
                    .build());
        } catch (Exception e) {
            // WebSocket broadcast failure should not prevent cancellation
            org.slf4j.LoggerFactory.getLogger(BookingController.class)
                    .warn("Failed to broadcast seat release for booking {}: {}", id, e.getMessage());
        }

        return ResponseEntity.ok("Đã hủy đơn đặt vé thành công.");
    }

    @PutMapping("/{id}/retry-payment")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> retryPayment(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));

        // Kiểm tra quyền sở hữu
        if (booking.getCustomer() == null || booking.getCustomer().getUser() == null
                || !booking.getCustomer().getUser().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thanh toán đơn đặt vé này.");
        }

        // Chỉ cho retry khi: status = PENDING_PAYMENT và paymentStatus = FAILED hoặc PENDING
        boolean canRetry = "PENDING_PAYMENT".equals(booking.getStatus())
                && ("FAILED".equals(booking.getPaymentStatus()) || "PENDING".equals(booking.getPaymentStatus()));
        if (!canRetry) {
            throw new ConflictException("Đơn đặt vé không ở trạng thái có thể thanh toán lại.");
        }

        // Kiểm tra hết hạn
        if (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
            booking.setStatus("CANCELLED");
            booking.setPaymentStatus("CANCELLED");
            bookingRepository.save(booking);
            throw new ConflictException("Đơn đặt vé đã hết thời gian thanh toán lại.");
        }

        // Re-lock ghế
        java.util.Set<com.opticine.entity.ShowtimeSeat> seats = booking.getSeats();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (com.opticine.entity.ShowtimeSeat seat : seats) {
            if ("BOOKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế đã được người khác đặt. Vui lòng tạo đơn mới.");
            }
            if ("LOCKED".equals(seat.getStatus()) && seat.getLockedBy() != null
                    && !seat.getLockedBy().equals(userDetails.getId())) {
                throw new ConflictException("Ghế đang được giữ bởi người khác. Vui lòng thử lại sau.");
            }
            seat.setStatus("LOCKED");
            seat.setLockedBy(userDetails.getId());
            seat.setLockedAt(now);
        }
        showtimeSeatRepository.saveAll(seats);

        // Reset payment status
        booking.setPaymentStatus("PENDING");
        booking.setExpiredAt(now.plusMinutes(10));
        bookingRepository.save(booking);

        // Broadcast seat locked
        try {
            seatEventPublisher.publish(booking.getShowtime().getId(),
                    com.opticine.dto.showtime.response.SeatEventDto.builder()
                            .type(com.opticine.dto.showtime.response.SeatEventDto.Type.SEAT_HELD)
                            .showtimeId(booking.getShowtime().getId())
                            .seatIds(seats.stream().map(s -> s.getSeat().getId()).toList())
                            .byUserId(userDetails.getId())
                            .build());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(BookingController.class)
                    .warn("Failed to broadcast seat lock for booking {}: {}", id, e.getMessage());
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("bookingId", booking.getId());
        response.put("paymentMethod", booking.getPaymentMethod());
        response.put("message", "Đã khôi phục đơn đặt vé. Vui lòng thanh toán.");
        return ResponseEntity.ok(response);
    }
}
