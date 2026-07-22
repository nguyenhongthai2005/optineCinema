package com.opticine.controller;

import com.opticine.dto.payment.VietQrCreateRequest;
import com.opticine.dto.payment.VietQrPaymentInfoResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.exception.ResourceNotFoundException;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import com.opticine.service.BookingPricingService;
import com.opticine.service.VietQrConfirmationService;
import com.opticine.service.VietQrService;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/payment/vietqr")
@RequiredArgsConstructor
public class VietQrPaymentController {

    private final BookingRepository bookingRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final VietQrService vietQrService;
    private final VietQrConfirmationService vietQrConfirmationService;
    private final BookingPricingService bookingPricingService;

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<?> create(@RequestBody VietQrCreateRequest request) {
        Booking booking = loadOwnedBooking(request != null ? request.getBookingId() : null, false);
        UserDetailsImpl userDetails = currentUser();
        refreshOwnedSeatLocks(booking, userDetails.getId());
        String transferContent = booking.getPaymentReference();
        if (transferContent == null || transferContent.isBlank()) {
            transferContent = vietQrService.generateTransferContent(booking.getId());
        }

            booking.setPaymentMethod("VIETQR");
            if (booking.getPaymentStatus() == null || booking.getPaymentStatus().isBlank() || "PENDING".equals(booking.getPaymentStatus())) {
                booking.setPaymentStatus("PENDING");
            }
            booking.setPaymentReference(transferContent);
            bookingPricingService.ensurePricingSnapshot(booking);
            bookingRepository.save(booking);

        VietQrPaymentInfoResponse response = vietQrService.buildPaymentInfo(booking);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-transferred")
    public ResponseEntity<?> markTransferred(@RequestBody VietQrCreateRequest request) {
        UserDetailsImpl userDetails = currentUser();
        return ResponseEntity.ok(vietQrConfirmationService.confirmCustomerTransfer(
                request != null ? request.getBookingId() : null,
                userDetails.getId()
        ));
    }

    private Booking loadOwnedBooking(Long bookingId, boolean allowConfirmed) {
        if (bookingId == null) {
            throw new BadRequestException("Thiếu mã đơn đặt vé.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));

        boolean isOwner = booking.getCustomer() != null
                && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getId().equals(userDetails.getId());
        if (!isOwner) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xác nhận thanh toán cho đơn này."
            );
        }
        if ("CONFIRMED".equals(booking.getStatus()) && !allowConfirmed) {
            throw new ConflictException("Đơn đặt vé không còn có thể thanh toán.");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new ConflictException("Đơn đặt vé không còn có thể thanh toán.");
        }
        bookingPricingService.ensurePricingSnapshot(booking);
        if (booking.getFinalAmount() == null || booking.getFinalAmount().signum() <= 0) {
            throw new BadRequestException("Số tiền thanh toán phải lớn hơn 0.");
        }
        return booking;
    }

    private UserDetailsImpl currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    private void refreshOwnedSeatLocks(Booking booking, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : booking.getSeats()) {
            if ("BOOKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế đã được đặt.");
            }
            if ("LOCKED".equals(seat.getStatus()) && seat.getLockedBy() != null && !seat.getLockedBy().equals(userId)) {
                throw new ConflictException("Ghế đang được giữ bởi người khác.");
            }
            seat.setStatus("LOCKED");
            seat.setLockedBy(userId);
            seat.setLockedAt(now);
        }
        showtimeSeatRepository.saveAll(booking.getSeats());
    }
}
