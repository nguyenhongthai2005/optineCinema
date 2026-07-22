package com.opticine.controller;

import com.opticine.dto.staff.AttendanceLocationRequest;
import com.opticine.dto.staff.StaffAvailabilityRequest;
import com.opticine.entity.StaffPosition;
import com.opticine.service.StaffService;
import com.opticine.service.StaffSchedulingService;
import jakarta.validation.Valid;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffController {
    private final StaffService staffService;
    private final StaffSchedulingService schedulingService;
    private final com.opticine.controller.AdminPaymentController adminPaymentController;
    private final com.opticine.controller.AdminTicketController adminTicketController;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<?> dashboard(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(staffService.dashboard(currentUser.getId()));
    }

    @GetMapping("/showtimes/today")
    public ResponseEntity<?> todayShowtimes() {
        return ResponseEntity.ok(staffService.todayShowtimes(LocalDate.now()));
    }

    @GetMapping("/showtimes")
    public ResponseEntity<?> showtimes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(staffService.todayShowtimes(date));
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<?> attendanceToday(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(staffService.attendanceToday(currentUser.getId()));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<?> checkIn(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody(required = false) AttendanceLocationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(staffService.checkIn(
                currentUser.getId(),
                request,
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<?> checkOut(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody(required = false) AttendanceLocationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(staffService.checkOut(
                currentUser.getId(),
                request,
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/attendance/my")
    public ResponseEntity<?> attendanceHistory(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(staffService.attendanceHistory(currentUser.getId(), fromDate, toDate));
    }

    @GetMapping("/customers/search")
    public ResponseEntity<?> searchCustomers(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(staffService.searchCustomers(keyword));
    }

    @PostMapping("/customers/quick-create")
    public ResponseEntity<?> quickCreateCustomer(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(staffService.quickCreateCustomer(request));
    }

    @GetMapping("/combos/active")
    public ResponseEntity<?> activeCombos(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return ResponseEntity.ok(staffService.activeCombos());
    }

    @PostMapping("/food-orders")
    public ResponseEntity<?> createFoodOrder(@AuthenticationPrincipal UserDetailsImpl currentUser, @RequestBody Map<String, Object> request) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        try {
            return ResponseEntity.ok(staffService.createFoodOrder(currentUser.getId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/food-orders")
    public ResponseEntity<?> foodOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword
    ) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return ResponseEntity.ok(staffService.foodOrders(date, keyword));
    }

    @PostMapping("/bookings/counter")
    public ResponseEntity<?> counterBooking(@AuthenticationPrincipal UserDetailsImpl currentUser, @RequestBody Map<String, Object> request) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        try {
            return ResponseEntity.ok(staffService.counterBooking(currentUser.getId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> orders(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return ResponseEntity.ok(staffService.orders(date, status, keyword));
    }

    @GetMapping("/payments/pending")
    public ResponseEntity<?> pendingPayments(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return adminPaymentController.pending();
    }

    @PostMapping("/payments/{bookingId}/confirm-vietqr")
    public ResponseEntity<?> confirmVietQr(@AuthenticationPrincipal UserDetailsImpl currentUser, @PathVariable Long bookingId) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return adminPaymentController.confirmVietQr(bookingId);
    }

    @PostMapping("/payments/{bookingId}/reject-vietqr")
    public ResponseEntity<?> rejectVietQr(@AuthenticationPrincipal UserDetailsImpl currentUser, @PathVariable Long bookingId) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.COUNTER_SALES);
        return adminPaymentController.rejectVietQr(bookingId);
    }

    @GetMapping("/tickets/verify")
    public ResponseEntity<?> verifyTicket(@AuthenticationPrincipal UserDetailsImpl currentUser, @RequestParam String qrCode) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.TICKET_CHECKER);
        return adminTicketController.verify(qrCode);
    }

    @PostMapping("/tickets/check-in")
    public ResponseEntity<?> checkInTicket(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                           @RequestBody com.opticine.dto.ticket.TicketCheckInRequest request) {
        staffService.requirePositionOrAdmin(currentUser, StaffPosition.TICKET_CHECKER);
        return adminTicketController.checkIn(request);
    }

    @GetMapping("/availability")
    public ResponseEntity<?> availability(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(schedulingService.myAvailability(currentUser.getId()));
    }

    @PostMapping("/availability")
    public ResponseEntity<?> createAvailability(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                @Valid @RequestBody StaffAvailabilityRequest request) {
        return ResponseEntity.ok(schedulingService.createAvailability(currentUser.getId(), request));
    }

    @PutMapping("/availability/{id}")
    public ResponseEntity<?> updateAvailability(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody StaffAvailabilityRequest request) {
        return ResponseEntity.ok(schedulingService.updateAvailability(currentUser.getId(), id, request));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<?> deleteAvailability(@AuthenticationPrincipal UserDetailsImpl currentUser, @PathVariable Long id) {
        schedulingService.deleteAvailability(currentUser.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thời gian làm việc."));
    }

    @GetMapping("/assignments/my")
    public ResponseEntity<?> myAssignments(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(schedulingService.myAssignments(currentUser.getId(), fromDate, toDate));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(staffService.profile(currentUser.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetailsImpl currentUser, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(staffService.updateProfile(currentUser.getId(), request));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetailsImpl currentUser, @RequestBody Map<String, Object> request) {
        try {
            staffService.changePassword(currentUser.getId(), request);
            return ResponseEntity.ok(Map.of("message", "Đã đổi mật khẩu."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
