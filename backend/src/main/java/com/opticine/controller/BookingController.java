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
}
