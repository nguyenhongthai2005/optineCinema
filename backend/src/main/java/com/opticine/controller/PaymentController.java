package com.opticine.controller;

import com.opticine.config.VNPayConfig;
import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.entity.*;
import com.opticine.exception.ApiException;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.exception.ResourceNotFoundException;
import com.opticine.repository.*;
import com.opticine.service.MembershipService;
import com.opticine.service.BookingPricingService;
import com.opticine.service.SeatEventPublisher;
import com.opticine.service.TicketEmailService;
import com.opticine.service.VietQrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.opticine.service.security.UserDetailsImpl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/payment/vnpay")
@Slf4j
public class PaymentController {

    @Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnp_HashSecret;

    @Value("${vnpay.url}")
    private String vnp_PayUrl;

    @Value("${vnpay.return-url}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.version}")
    private String vnp_Version;

    @Value("${vnpay.command}")
    private String vnp_Command;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private SeatEventPublisher seatEventPublisher;

    @Autowired
    private TicketEmailService ticketEmailService;

    @Autowired
    private VietQrService vietQrService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private BookingPricingService bookingPricingService;

    @GetMapping("/create-url")
    public ResponseEntity<?> createPaymentUrl(@RequestParam Long bookingId,
                                              HttpServletRequest request,
                                              @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));
        if (!canPayBooking(booking, currentUser)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thanh toán đơn này.");
        }

        if ("CANCELLED".equals(booking.getStatus()) || "CONFIRMED".equals(booking.getStatus())) {
            throw new ConflictException("Đơn đặt vé không còn có thể thanh toán.");
        }

            bookingPricingService.ensurePricingSnapshot(booking);
            bookingRepository.save(booking);
            BigDecimal totalAmount = booking.getFinalAmount();
            long amount = totalAmount.longValue() * 100;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", bookingId.toString() + "_" + System.currentTimeMillis());
            vnp_Params.put("vnp_OrderInfo", "ThanhToanDonHang" + bookingId);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(VNPayConfig.encode(fieldValue));
                    query.append(VNPayConfig.encode(fieldName));
                    query.append('=');
                    query.append(VNPayConfig.encode(fieldValue));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String hashDataStr = hashData.toString();
            String vnp_SecureHash = VNPayConfig.hmacSHA512(vnp_HashSecret.trim(), hashDataStr);
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = vnp_PayUrl + "?" + queryUrl;

            Map<String, String> response = new HashMap<>();
            booking.setPaymentMethod("VNPAY");
            booking.setPaymentStatus("PENDING");
            bookingRepository.save(booking);

        response.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Xu ly logic xac nhan thanh toan (dung chung cho Return va IPN)
    // ==========================================
    @Transactional
    protected boolean processPaymentConfirmation(Booking booking, String vnpAmount) {
        // Chi xu ly neu Booking chua duoc CONFIRMED (tranh tao dup Invoice/Ticket)
        if ("CONFIRMED".equals(booking.getStatus())) {
            return true; // Da xu ly truoc do (co the IPN chay truoc Return)
        }

        bookingPricingService.ensurePricingSnapshot(booking, true);
        BigDecimal totalAmount = booking.getFinalAmount();
        if (vnpAmount != null && !vnpAmount.isBlank()) {
            long expectedAmount = totalAmount.longValue() * 100;
            long paidAmount = Long.parseLong(vnpAmount);
            if (expectedAmount != paidAmount) {
                throw new BadRequestException("Số tiền thanh toán không hợp lệ.");
            }
        }
        booking.setStatus("CONFIRMED");
        booking.setPaymentMethod("VNPAY");
        booking.setPaymentStatus("PAID");
        booking.setPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Generate Invoice
        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElseGet(Invoice::new);
        invoice.setBooking(booking);
        invoice.setCustomer(booking.getCustomer());
        invoice.setShowtime(booking.getShowtime());
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus("PAID");
        invoice.setCustomerUsername(customerUsername(booking));
        invoice.setMembershipProcessed(invoice.getMembershipProcessed() != null ? invoice.getMembershipProcessed() : false);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Cộng điểm và cập nhật hạng thành viên.
        membershipService.processInvoice(savedInvoice);

        Payment payment = paymentRepository.findTopByInvoiceBookingIdOrderByCreatedAtDesc(booking.getId()).orElseGet(Payment::new);
        payment.setInvoice(invoice);
        payment.setPaymentMethod("VNPAY");
        payment.setTransactionCode(booking.getId().toString());
        payment.setAmount(totalAmount);
        payment.setOriginalAmount(totalAmount);
        payment.setPayableAmount(totalAmount);
        payment.setPaidAmount(totalAmount);
        payment.setStatus("PAID");
        payment.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now());
        payment.setPaidAt(booking.getPaidAt());
        paymentRepository.save(payment);

        List<Ticket> createdTickets = new ArrayList<>();

        // Update Seats to BOOKED and Create Tickets
        for (ShowtimeSeat sts : booking.getSeats()) {
            if ("BOOKED".equals(sts.getStatus())) {
                continue;
            }
            if (!"LOCKED".equals(sts.getStatus())) {
                throw new ConflictException("Ghế không còn được giữ cho đơn đặt vé: " + sts.getId());
            }
            sts.setStatus("BOOKED");
            sts.setLockedBy(null);
            sts.setLockedAt(null);
            showtimeSeatRepository.save(sts);
            
            // Create Ticket
            Ticket ticket = new Ticket();
            ticket.setInvoice(invoice);
            ticket.setShowtimeSeat(sts);
            ticket.setPrice(calculateSeatPrice(booking, sts));
            ticket.setQrCode(UUID.randomUUID().toString());
            ticket.setStatus("VALID");
            createdTickets.add(ticketRepository.save(ticket));
        }
        publishSeatEvent(booking, SeatEventDto.Type.SEAT_SOLD);
        sendTicketEmailIfNeeded(booking, createdTickets);
        return true;
    }

    private void sendTicketEmailIfNeeded(Booking booking, List<Ticket> tickets) {
        if (Boolean.TRUE.equals(booking.getTicketEmailSent())) {
            return;
        }
        if (tickets == null || tickets.isEmpty()) {
            tickets = ticketRepository.findByShowtimeSeatIn(booking.getSeats());
        }
        try {
            ticketEmailService.sendTicketEmail(booking, tickets);
            booking.setTicketEmailSent(true);
            bookingRepository.save(booking);
        } catch (Exception e) {
            log.warn("Ticket email failed for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    @Transactional
    private void processPaymentFailure(Booking booking) {
        if ("CANCELLED".equals(booking.getStatus())) {
            return; // Da xu ly truoc do
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        // Unlock Seats
        for (ShowtimeSeat sts : booking.getSeats()) {
            if (!"BOOKED".equals(sts.getStatus())) {
                sts.setStatus("AVAILABLE");
                sts.setLockedBy(null);
                sts.setLockedAt(null);
                showtimeSeatRepository.save(sts);
            }
        }
        publishSeatEvent(booking, SeatEventDto.Type.SEAT_RELEASED);
    }

    // ==========================================
    // VNPay Return URL (User redirect ve Frontend)
    // ==========================================
    @GetMapping("/return")
    @Transactional
    public ResponseEntity<?> paymentReturn(HttpServletRequest request) {
        try {
            Map<String, String> fields = extractVnpayFields(request);
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            String signValue = buildSignature(fields);
            
            String txnRef = request.getParameter("vnp_TxnRef");
            Long bookingId = Long.parseLong(txnRef.split("_")[0]);

            if (signValue.equals(vnp_SecureHash)) {
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé."));
                if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                    processPaymentConfirmation(booking, request.getParameter("vnp_Amount"));
                    return ResponseEntity.status(302)
                            .header("Location", frontendUrl + "/payment-result?bookingId=" + bookingId + "&status=success")
                            .build();
                } else {
                    processPaymentFailure(booking);
                    return ResponseEntity.status(302)
                            .header("Location", frontendUrl + "/payment-result?bookingId=" + bookingId + "&status=failed")
                            .build();
                }
            } else {
                throw new BadRequestException("Chữ ký VNPay không hợp lệ.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Dữ liệu phản hồi từ VNPay không hợp lệ.");
        }
    }

    // ==========================================
    // VNPay IPN (Server-to-Server callback)
    // ==========================================
    @GetMapping("/ipn")
    @Transactional
    public ResponseEntity<?> paymentIpn(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> fields = extractVnpayFields(request);
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            String signValue = buildSignature(fields);

            if (!signValue.equals(vnp_SecureHash)) {
                result.put("RspCode", "97");
                result.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(result);
            }

            String txnRef = request.getParameter("vnp_TxnRef");
            Long bookingId = Long.parseLong(txnRef.split("_")[0]);

            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                result.put("RspCode", "01");
                result.put("Message", "Order not found");
                return ResponseEntity.ok(result);
            }

            bookingPricingService.ensurePricingSnapshot(booking);
            BigDecimal totalAmount = booking.getFinalAmount();
            long expectedAmount = totalAmount.longValue() * 100;
            long vnpAmount = Long.parseLong(request.getParameter("vnp_Amount"));
            if (expectedAmount != vnpAmount) {
                result.put("RspCode", "04");
                result.put("Message", "Invalid Amount");
                return ResponseEntity.ok(result);
            }

            // Kiem tra da xu ly chua
            if ("CONFIRMED".equals(booking.getStatus())) {
                result.put("RspCode", "02");
                result.put("Message", "Order already confirmed");
                return ResponseEntity.ok(result);
            }

            if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                processPaymentConfirmation(booking, request.getParameter("vnp_Amount"));
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            } else {
                processPaymentFailure(booking);
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
            return ResponseEntity.ok(result);
        }
    }

    // ==========================================
    // Helper methods
    // ==========================================
    private Map<String, String> extractVnpayFields(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }
        return fields;
    }

    private String buildSignature(Map<String, String> fields) throws Exception {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return VNPayConfig.hmacSHA512(vnp_HashSecret.trim(), hashData.toString());
    }

    private BigDecimal calculateBookingTotal(Booking booking) {
        bookingPricingService.ensurePricingSnapshot(booking);
        return booking.getFinalAmount();
    }

    private BigDecimal calculateSeatPrice(Booking booking, ShowtimeSeat sts) {
        return bookingPricingService.calculateSeatPrice(booking, sts);
    }

    private String customerUsername(Booking booking) {
        if (booking.getCustomer() == null) return "";
        if (booking.getCustomer().getUser() != null) return booking.getCustomer().getUser().getUsername();
        if (booking.getCustomer().getPhone() != null) return booking.getCustomer().getPhone();
        return booking.getCustomer().getEmail() != null ? booking.getCustomer().getEmail() : "";
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

    private boolean canPayBooking(Booking booking, UserDetailsImpl currentUser) {
        if (currentUser == null) return false;
        boolean canManage = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));
        if (canManage) return true;
        return booking.getCustomer() != null
                && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getId().equals(currentUser.getId());
    }
}
