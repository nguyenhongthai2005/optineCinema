package com.opticine.service;

import com.opticine.dto.booking.BookingRequest;
import com.opticine.dto.booking.BookingResponse;
import com.opticine.dto.booking.ComboItemResponse;
import com.opticine.dto.booking.MyBookingResponse;
import com.opticine.dto.promotion.PromotionValidateRequest;
import com.opticine.dto.promotion.PromotionValidateResponse;
import com.opticine.entity.*;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.exception.ResourceNotFoundException;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.PaymentRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private ComboService comboService;

    @Autowired
    private VietQrService vietQrService;

    @Autowired
    private BookingPricingService bookingPricingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private ShowtimeAvailabilityService showtimeAvailabilityService;

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        if (request.getShowtimeSeatIds() == null || request.getShowtimeSeatIds().isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ghế.");
        }

        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng."));

        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(request.getShowtimeSeatIds());
        if (seats.isEmpty() || seats.size() != request.getShowtimeSeatIds().size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều ghế không tồn tại.");
        }

        Showtime showtime = seats.get(0).getShowtime();
        if (showtime == null) {
            throw new BadRequestException("Ghế đã chọn chưa được gán vào suất chiếu.");
        }
        showtimeAvailabilityService.requireBookableForBooking(showtime);

        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : seats) {
            if (seat.getShowtime() == null || !showtime.getId().equals(seat.getShowtime().getId())) {
                throw new BadRequestException("Tất cả ghế phải thuộc cùng một suất chiếu.");
            }
            if (seat.getSeat() != null && "MAINTENANCE".equalsIgnoreCase(seat.getSeat().getStatus())) {
                throw new ConflictException("Ghế đang bảo trì, vui lòng chọn ghế khác.");
            }
            if ("BOOKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế đã được đặt: " + formatSeat(seat));
            }
            if (!"LOCKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế phải được giữ trước khi đặt: " + formatSeat(seat));
            }
            if (seat.getLockedBy() == null || !seat.getLockedBy().equals(userId)) {
                throw new ConflictException("Ghế đang được giữ bởi người khác: " + formatSeat(seat));
            }
            if (seat.getLockedAt() == null || seat.getLockedAt().plusMinutes(2).isBefore(now)) {
                throw new ConflictException("Thời gian giữ ghế đã hết: " + formatSeat(seat));
            }
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking.setCreatedAt(now);
        booking.setExpiredAt(seats.stream()
                .map(seat -> seat.getLockedAt().plusMinutes(2))
                .min(LocalDateTime::compareTo)
                .orElse(now.plusMinutes(2)));
        
        Set<ShowtimeSeat> seatSet = new HashSet<>(seats);
        booking.setSeats(seatSet);

        Booking saved = bookingRepository.save(booking);
        List<BookingCombo> comboItems = comboService.buildBookingCombos(saved, request.getCombos());
        saved.getComboItems().clear();
        saved.getComboItems().addAll(comboItems);

        String promoCode = request.getPromotionCode();
        BigDecimal ticketTotal = bookingPricingService.calculateTicketTotal(saved);
        BigDecimal comboTotal = comboService.comboTotal(saved.getComboItems());
        BigDecimal discountAmount = calculateDiscountOrThrow(userId, promoCode, ticketTotal, comboTotal);
        bookingPricingService.applyPricingSnapshot(saved, discountAmount, promoCode);
        saved = bookingRepository.save(saved);

        if (promoCode != null && !promoCode.isBlank()) {
            promotionService.applyPromotion(promoCode, userId, saved);
        }

        return toBookingResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse previewBooking(Long userId, BookingRequest request) {
        if (request.getShowtimeSeatIds() == null || request.getShowtimeSeatIds().isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ghế.");
        }

        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng."));
        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(request.getShowtimeSeatIds());
        if (seats.isEmpty() || seats.size() != request.getShowtimeSeatIds().size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều ghế không tồn tại.");
        }

        Showtime showtime = seats.get(0).getShowtime();
        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : seats) {
            if (seat.getShowtime() == null || showtime == null || !showtime.getId().equals(seat.getShowtime().getId())) {
                throw new BadRequestException("Tất cả ghế phải thuộc cùng một suất chiếu.");
            }
            if ("BOOKED".equals(seat.getStatus())) {
                throw new ConflictException("Ghế đã được đặt: " + formatSeat(seat));
            }
            if ("LOCKED".equals(seat.getStatus())
                    && seat.getLockedBy() != null
                    && !seat.getLockedBy().equals(userId)) {
                throw new ConflictException("Ghế đang được giữ bởi người khác: " + formatSeat(seat));
            }
            if ("LOCKED".equals(seat.getStatus())
                    && seat.getLockedAt() != null
                    && seat.getLockedAt().plusMinutes(2).isBefore(now)) {
                throw new ConflictException("Đơn đặt vé đã hết hạn thanh toán.");
            }
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setSeats(new HashSet<>(seats));
        booking.getComboItems().addAll(comboService.buildBookingCombos(booking, request.getCombos()));

        BigDecimal ticketTotal = bookingPricingService.calculateTicketTotal(booking);
        BigDecimal comboTotal = comboService.comboTotal(booking.getComboItems());
        BigDecimal voucherDiscountAmount = calculateDiscountOrThrow(userId, request.getPromotionCode(), ticketTotal, comboTotal);
        bookingPricingService.applyPricingSnapshot(booking, voucherDiscountAmount, request.getPromotionCode());
        return toBookingResponse(booking);
    }

    private String formatSeat(ShowtimeSeat seat) {
        if (seat.getSeat() == null) {
            return String.valueOf(seat.getId());
        }
        return seat.getSeat().getRowLabel() + seat.getSeat().getColumnNumber();
    }

    public List<MyBookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByCustomerUserIdOrderByCreatedAtDesc(userId).stream().map(booking -> {
            MyBookingResponse res = new MyBookingResponse();
            res.setId(booking.getId());
            res.setStatus(booking.getStatus());
            res.setPaymentMethod(booking.getPaymentMethod());
            res.setPaymentStatus(booking.getPaymentStatus());
            res.setPaymentReference(booking.getPaymentReference());
            res.setCreatedAt(booking.getCreatedAt());
            res.setExpiredAt(booking.getExpiredAt());
            res.setSeatCount(booking.getSeats() != null ? booking.getSeats().size() : 0);

            Showtime showtime = booking.getShowtime();
            if (showtime != null) {
                // Kiểm tra null an toàn cho phim và phòng chiếu.
                if (showtime.getMovie() != null) {
                    res.setMovieTitle(showtime.getMovie().getTitle());
                } else {
                    res.setMovieTitle("Đang cập nhật");
                }
                if (showtime.getRoom() != null) {
                    res.setRoomName(showtime.getRoom().getName());
                } else {
                    res.setRoomName("Phòng chưa xác định");
                }
                res.setStartTime(showtime.getStartTime());
            }

            bookingPricingService.ensurePricingSnapshot(booking);
            BigDecimal ticketTotal = booking.getTicketTotal();
            BigDecimal comboTotal = booking.getComboTotal();
            List<ComboItemResponse> comboItems = comboService.bookingComboResponses(booking.getId());
            res.setTicketTotal(ticketTotal);
            res.setComboTotal(comboTotal);
            res.setGrossTotal(booking.getGrossTotal());
            res.setVoucherDiscountAmount(booking.getVoucherDiscountAmount());
            res.setMembershipTierName(booking.getMembershipTierName());
            res.setMembershipDiscountPercent(booking.getMembershipDiscountPercent());
            res.setMembershipDiscountAmount(booking.getMembershipDiscountAmount());
            BigDecimal discountAmount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal finalAmount = booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO;
            res.setDiscountAmount(discountAmount);
            res.setPromotionCode(booking.getPromotionCode());
            res.setComboItems(comboItems);
            res.setTotalAmount(finalAmount);
            if ("VIETQR".equals(booking.getPaymentMethod())) {
                paymentRepository.findTopByInvoiceBookingIdOrderByCreatedAtDesc(booking.getId()).ifPresent(payment -> {
                    res.setVietQrPayableAmount(payment.getPayableAmount() != null ? payment.getPayableAmount() : payment.getAmount());
                    res.setVietQrPaidAmount(payment.getPaidAmount());
                    res.setVietQrDemoDiscountAmount(payment.getDemoDiscountAmount());
                    res.setVietQrDemoMode(payment.getDemoMode());
                });
            }
            return res;
        }).collect(Collectors.toList());
    }

    private BookingResponse toBookingResponse(Booking booking) {
        bookingPricingService.ensurePricingSnapshot(booking);
        BigDecimal ticketTotal = booking.getTicketTotal();
        BigDecimal comboTotal = booking.getComboTotal();
        BigDecimal discountAmount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BookingResponse res = new BookingResponse();
        res.setId(booking.getId());
        res.setStatus(booking.getStatus());
        res.setExpiredAt(booking.getExpiredAt());
        res.setShowtimeSeatIds(booking.getSeats().stream().map(ShowtimeSeat::getId).collect(Collectors.toList()));
        res.setComboItems(comboService.bookingComboResponses(booking.getComboItems()));
        res.setTicketTotal(ticketTotal);
        res.setComboTotal(comboTotal);
        res.setGrossTotal(booking.getGrossTotal());
        res.setVoucherDiscountAmount(booking.getVoucherDiscountAmount());
        res.setMembershipTierName(booking.getMembershipTierName());
        res.setMembershipDiscountPercent(booking.getMembershipDiscountPercent());
        res.setMembershipDiscountAmount(booking.getMembershipDiscountAmount());
        res.setDiscountAmount(discountAmount);
        res.setFinalAmount(booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO);
        res.setTotalAmount(booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO);
        res.setPromotionCode(booking.getPromotionCode());
        return res;
    }

    private BigDecimal calculateDiscountOrThrow(Long userId, String promoCode, BigDecimal ticketTotal, BigDecimal comboTotal) {
        if (promoCode == null || promoCode.isBlank()) return BigDecimal.ZERO;
        PromotionValidateRequest validateRequest = new PromotionValidateRequest();
        validateRequest.setCode(promoCode);
        validateRequest.setTicketAmount(ticketTotal);
        validateRequest.setComboAmount(comboTotal);
        PromotionValidateResponse result = promotionService.validate(userId, validateRequest);
        if (!result.isValid()) {
            throw new BadRequestException("Mã khuyến mãi không hợp lệ hoặc đã hết hạn.");
        }
        return result.getDiscountAmount() != null ? result.getDiscountAmount() : BigDecimal.ZERO;
    }

    private BigDecimal calculateTicketTotal(Booking booking) {
        return bookingPricingService.calculateTicketTotal(booking);
    }
}
