package com.opticine.service;

import com.opticine.dto.staff.AttendanceLocationRequest;
import com.opticine.dto.booking.ComboItemRequest;
import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.dto.combo.ComboResponse;
import com.opticine.entity.*;
import com.opticine.exception.BadRequestException;
import com.opticine.exception.ConflictException;
import com.opticine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.opticine.service.security.UserDetailsImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {
    private static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(17, 0);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final ComboRepository comboRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final ComboService comboService;
    private final VietQrService vietQrService;
    private final SeatEventPublisher seatEventPublisher;
    private final TicketEmailService ticketEmailService;
    private final PasswordEncoder passwordEncoder;
    private final LocationService locationService;
    private final StaffSchedulingService staffSchedulingService;
    private final ShowtimeStatusService showtimeStatusService;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;
    private final MembershipService membershipService;
    private final BookingPricingService bookingPricingService;

    @Value("${app.attendance.location-check-enabled:true}")
    private boolean attendanceLocationCheckEnabled;

    @Value("${app.attendance.default-location-name:Opticine Cinema}")
    private String attendanceLocationName;

    @Value("${app.attendance.center-latitude:${app.attendance.default-latitude:17.467256}}")
    private BigDecimal attendanceLatitude;

    @Value("${app.attendance.center-longitude:${app.attendance.default-longitude:106.587302}}")
    private BigDecimal attendanceLongitude;

    @Value("${app.attendance.radius-meters:${app.attendance.allowed-radius-meters:10000}}")
    private Integer attendanceAllowedRadiusMeters;

    @Value("${app.attendance.max-accuracy-meters:100}")
    private BigDecimal attendanceMaxAccuracyMeters;

    @Value("${app.attendance.strict-accuracy-check:false}")
    private boolean strictAccuracyCheck;

    @Value("${app.attendance.early-checkin-minutes:15}")
    private Integer earlyCheckInMinutes;

    @Value("${app.attendance.late-grace-minutes:5}")
    private Integer lateGraceMinutes;

    public Map<String, Object> dashboard(Long staffId) {
        User staff = findUser(staffId);
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        Map<String, Object> attendance = attendanceToday(staffId);
        List<Map<String, Object>> todayAssignments = staffSchedulingService.myAssignments(staffId, today, today);
        List<Map<String, Object>> nextAssignments = staffSchedulingService.myAssignments(staffId, today, today.plusDays(14));
        List<Map<String, Object>> upcoming = todayShowtimes(today).stream()
                .filter(item -> ((LocalDateTime) item.get("startTime")).isAfter(LocalDateTime.now()))
                .limit(5)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("staffName", staff.getFullName());
        response.put("position", staff.getStaffPosition());
        response.put("positionLabel", positionLabel(staff.getStaffPosition()));
        response.put("contractType", staff.getEmploymentType());
        response.put("contractTypeLabel", contractTypeLabel(staff.getEmploymentType()));
        response.put("todayShowtimes", showtimeRepository.findByStartTimeBetween(from, to).size());
        response.put("ticketsSoldToday", ticketRepository.countByInvoiceBookingCreatedAtBetween(from, to));
        response.put("checkedInTicketsToday", ticketRepository.countByCheckedInAtBetween(from, to));
        response.put("pendingVietQrPayments", bookingRepository.findByPaymentMethodAndPaymentStatusOrderByCreatedAtDesc("VIETQR", "WAITING_CONFIRMATION").size());
        response.put("upcomingShowtimesToday", upcoming);
        response.put("todayAttendanceStatus", attendance.get("status"));
        response.put("checkInTime", attendance.get("checkInTime"));
        response.put("checkOutTime", attendance.get("checkOutTime"));
        response.put("canCheckIn", attendance.get("canCheckIn"));
        response.put("canCheckOut", attendance.get("canCheckOut"));
        response.put("shiftStart", attendance.get("shiftStart"));
        response.put("shiftEnd", attendance.get("shiftEnd"));
        response.put("checkInAvailableFrom", attendance.get("checkInAvailableFrom"));
        response.put("checkInBlockedReason", attendance.get("checkInBlockedReason"));
        response.put("locationCheckEnabled", attendanceLocationCheckEnabled);
        response.put("workplaceName", attendanceLocationName);
        response.put("allowedRadiusMeters", attendanceAllowedRadiusMeters);
        response.put("todayAssignments", todayAssignments);
        response.put("nextAssignment", nextAssignments.stream().findFirst().orElse(null));
        response.put("availabilityCount", staffSchedulingService.myAvailability(staffId).size());
        return response;
    }

    public List<Map<String, Object>> todayShowtimes(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return showtimeRepository.findByStartTimeBetween(target.atStartOfDay(), target.plusDays(1).atStartOfDay())
                .stream()
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .map(this::toShowtimeMap)
                .toList();
    }

    public Map<String, Object> attendanceToday(Long staffId) {
        User staff = findUser(staffId);
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByStaffIdAndBusinessDate(staffId, today)
                .map(this::toAttendanceMap)
                .orElseGet(() -> emptyAttendance(staff, today));
    }

    @Transactional
    public Map<String, Object> checkIn(Long staffId, AttendanceLocationRequest locationRequest, String ipAddress, String userAgent) {
        User staff = findUser(staffId);
        LocalDate today = LocalDate.now();
        StaffAssignment assignment = staffSchedulingService.findTodayAssignment(staffId, today);
        if (assignment == null) {
            throw new ConflictException("Bạn chưa được phân ca hôm nay. Vui lòng liên hệ quản lý.");
        }
        Attendance attendance = attendanceRepository.findByStaffIdAndBusinessDate(staffId, today).orElse(null);
        if (attendance != null && attendance.getCheckInTime() != null) {
            throw new ConflictException("Bạn đã check-in hôm nay.");
        }
        if (attendance == null) {
            attendance = new Attendance();
            attendance.setStaff(staff);
            attendance.setBusinessDate(today);
        }
        applyAssignmentShift(attendance, assignment);
        LocalDateTime now = LocalDateTime.now();
        LocalTime shiftStart = parseShiftTime(attendance.getShiftStartTime(), DEFAULT_SHIFT_START);
        if (now.toLocalTime().isBefore(checkInAvailableFrom(shiftStart))) {
            throw new BadRequestException("Bạn có thể check-in từ " + checkInAvailableFrom(shiftStart) + ".");
        }
        attendance.setCheckInTime(now);
        int lateMinutes = isLate(now.toLocalTime(), shiftStart) ? Math.max(0, (int) Duration.between(shiftStart, now.toLocalTime()).toMinutes()) : 0;
        attendance.setLateMinutes(lateMinutes);
        attendance.setWorkedMinutes(0);
        attendance.setEarlyLeaveMinutes(0);
        attendance.setStatus(lateMinutes > 0 ? "LATE" : "CHECKED_IN");
        attendance.setAutoMarkedAbsent(false);
        applyCheckInLocation(attendance, validateLocation(locationRequest), ipAddress, userAgent);
        return toAttendanceMap(attendanceRepository.save(attendance));
    }

    @Transactional
    public Map<String, Object> checkOut(Long staffId, AttendanceLocationRequest locationRequest, String ipAddress, String userAgent) {
        Attendance attendance = attendanceRepository.findByStaffIdAndBusinessDate(staffId, LocalDate.now())
                .orElseThrow(() -> new ConflictException("Bạn chưa check-in."));
        if (attendance.getCheckInTime() == null) {
            throw new ConflictException("Bạn chưa check-in.");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new ConflictException("Bạn đã check-out hôm nay.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalTime shiftEnd = parseShiftTime(attendance.getShiftEndTime(), DEFAULT_SHIFT_END);
        attendance.setCheckOutTime(now);
        attendance.setWorkedMinutes((int) Duration.between(attendance.getCheckInTime(), now).toMinutes());
        attendance.setEarlyLeaveMinutes(Math.max(0, (int) Duration.between(now.toLocalTime(), shiftEnd).toMinutes()));
        boolean wasLate = attendance.getLateMinutes() != null && attendance.getLateMinutes() > 0;
        boolean early = attendance.getEarlyLeaveMinutes() != null && attendance.getEarlyLeaveMinutes() > 0;
        attendance.setStatus(early ? "EARLY_LEAVE" : wasLate ? "LATE" : "COMPLETED");
        applyCheckOutLocation(attendance, validateLocation(locationRequest), ipAddress, userAgent);
        return toAttendanceMap(attendanceRepository.save(attendance));
    }

    public List<Map<String, Object>> attendanceHistory(Long staffId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(30);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        return attendanceRepository.search(staffId, from, to, null).stream()
                .map(this::toAttendanceMap)
                .toList();
    }

    public List<Map<String, Object>> searchCustomers(String keyword) {
        return customerRepository.searchCustomers(StringUtils.hasText(keyword) ? keyword.trim() : null)
                .stream()
                .limit(12)
                .map(customer -> Map.<String, Object>of(
                        "id", customer.getId(),
                        "fullName", value(customer.getFullName()),
                        "phone", value(customer.getPhone()),
                        "email", value(customer.getEmail())
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> quickCreateCustomer(Map<String, Object> request) {
        return toCustomerMap(createOrFindCustomer(request));
    }

    private Customer createOrFindCustomer(Map<String, Object> request) {
        String phone = stringValue(request.get("phone"));
        String email = stringValue(request.get("email"));
        if (StringUtils.hasText(phone)) {
            Optional<Customer> existing = customerRepository.findByPhone(phone);
            if (existing.isPresent()) return existing.get();
        }
        if (StringUtils.hasText(email)) {
            Optional<Customer> existing = customerRepository.findByEmail(email);
            if (existing.isPresent()) return existing.get();
        }
        Customer customer = new Customer();
        customer.setFullName(StringUtils.hasText(stringValue(request.get("fullName"))) ? stringValue(request.get("fullName")) : "Khách vãng lai");
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setTotalSpent(BigDecimal.ZERO);
        customer.setPoints(0);
        return customerRepository.save(customer);
    }

    public List<ComboResponse> activeCombos() {
        return comboService.activeCombos();
    }

    @Transactional
    public Map<String, Object> counterBooking(Long staffId, Map<String, Object> request) {
        User staff = findUser(staffId);
        Long showtimeId = longValue(request.get("showtimeId"));
        List<Long> showtimeSeatIds = longList(request.get("lockedShowtimeSeatIds"));
        String paymentMethod = Optional.ofNullable(stringValue(request.get("paymentMethod"))).orElse("CASH").toUpperCase(Locale.ROOT);
        if (showtimeId == null || showtimeSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn suất chiếu và ghế.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        if (!showtimeAvailabilityService.isBookable(showtime)) {
            throw new IllegalArgumentException("Suất chiếu đã bắt đầu hoặc đã hết thời gian bán vé.");
        }
        Customer customer = resolveCounterCustomer(request);
        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(showtimeSeatIds);
        validateCounterSeats(showtime, seats, showtimeSeatIds, staffId);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setSeats(new HashSet<>(seats));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(10));
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentReference("VIETQR".equals(paymentMethod) ? null : "CASH-" + System.currentTimeMillis());
        booking.setStatus("VIETQR".equals(paymentMethod) ? "WAITING_CONFIRMATION" : "CONFIRMED");
        booking.setPaymentStatus("VIETQR".equals(paymentMethod) ? "WAITING_CONFIRMATION" : "PAID");
        booking.setPaidAt("VIETQR".equals(paymentMethod) ? null : LocalDateTime.now());
        booking = bookingRepository.save(booking);
        booking.getComboItems().addAll(comboService.buildBookingCombos(booking, comboRequests(request.get("combos"))));
        bookingPricingService.ensurePricingSnapshot(booking, true);
        booking = bookingRepository.save(booking);

        BigDecimal total = booking.getFinalAmount();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookingId", booking.getId());
        response.put("bookingCode", bookingCode(booking));
        response.put("totalAmount", total);
        response.put("finalAmount", total);
        response.put("grossTotal", booking.getGrossTotal());
        response.put("voucherDiscountAmount", booking.getVoucherDiscountAmount());
        response.put("membershipTierName", booking.getMembershipTierName());
        response.put("membershipDiscountPercent", booking.getMembershipDiscountPercent());
        response.put("membershipDiscountAmount", booking.getMembershipDiscountAmount());
        response.put("discountAmount", booking.getDiscountAmount());
        response.put("paymentStatus", booking.getPaymentStatus());
        response.put("paymentMethod", booking.getPaymentMethod());
        response.put("movieTitle", showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Đang cập nhật");
        response.put("showtimeStart", showtime.getStartTime());
        response.put("showtimeEnd", showtime.getEndTime());
        response.put("roomName", showtime.getRoom() != null ? showtime.getRoom().getName() : "Phòng chưa xác định");
        response.put("seats", seats.stream().map(this::seatLabel).sorted().toList());

        if ("VIETQR".equals(paymentMethod)) {
            booking.setPaymentReference(vietQrService.generateTransferContent(booking.getId()));
            bookingRepository.save(booking);
            response.put("vietQr", vietQrService.buildPaymentInfo(booking));
            response.put("message", "Đơn đã được tạo và đang chờ xác nhận VietQR.");
            return response;
        }

        Invoice invoice = createInvoiceAndPayment(booking, staff, total, paymentMethod);
        List<Ticket> tickets = createMissingTickets(booking, invoice);
        markSeatsBooked(booking, staffId);
        sendTicketEmailIfPossible(booking, tickets);
        Booking savedBooking = booking;
        response.put("invoiceId", invoice.getId());
        response.put("ticketIds", tickets.stream().map(Ticket::getId).toList());
        response.put("tickets", tickets.stream()
                .sorted(Comparator.comparing(this::seatLabel))
                .map(ticket -> toCounterTicketMap(ticket, savedBooking))
                .toList());
        response.put("message", "Bán vé tiền mặt thành công.");
        return response;
    }

    @Transactional
    public Map<String, Object> createFoodOrder(Long staffId, Map<String, Object> request) {
        User staff = findUser(staffId);
        String paymentMethod = Optional.ofNullable(stringValue(request.get("paymentMethod"))).orElse("CASH").toUpperCase(Locale.ROOT);
        if (!"CASH".equals(paymentMethod)) {
            throw new IllegalArgumentException("Hiện tại đơn bắp nước tại quầy chỉ hỗ trợ tiền mặt.");
        }
        List<ComboItemRequest> comboRequests = comboService.normalizeRequests(comboRequests(request.get("combos")));
        if (comboRequests.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một combo.");
        }
        Map<Long, Combo> combos = comboRepository.findAllById(comboRequests.stream().map(ComboItemRequest::getComboId).toList())
                .stream()
                .collect(Collectors.toMap(Combo::getId, Function.identity()));

        FoodOrder order = new FoodOrder();
        order.setStaff(staff);
        order.setCustomerName(Optional.ofNullable(stringValue(request.get("customerName"))).filter(StringUtils::hasText).orElse("Khách vãng lai"));
        order.setCustomerPhone(stringValue(request.get("customerPhone")));
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PAID");
        order.setStatus("COMPLETED");
        order.setPaidAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (ComboItemRequest itemRequest : comboRequests) {
            Combo combo = combos.get(itemRequest.getComboId());
            if (combo == null) throw new IllegalArgumentException("Không tìm thấy combo.");
            if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) {
                throw new IllegalArgumentException("Combo không còn bán: " + combo.getName());
            }
            BigDecimal unitPrice = combo.getPrice() != null ? combo.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            FoodOrderItem item = new FoodOrderItem();
            item.setFoodOrder(order);
            item.setCombo(combo);
            item.setComboNameSnapshot(combo.getName());
            item.setUnitPriceSnapshot(unitPrice);
            item.setQuantity(itemRequest.getQuantity());
            item.setSubtotal(subtotal);
            order.getItems().add(item);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
        return toFoodOrderMap(foodOrderRepository.save(order));
    }

    public List<Map<String, Object>> foodOrders(LocalDate date, String keyword) {
        LocalDate target = date != null ? date : LocalDate.now();
        return foodOrderRepository.search(
                        target.atStartOfDay(),
                        target.plusDays(1).atStartOfDay(),
                        StringUtils.hasText(keyword) ? keyword.trim() : null)
                .stream()
                .map(this::toFoodOrderMap)
                .toList();
    }

    public List<Map<String, Object>> orders(LocalDate date, String status, String keyword) {
        LocalDate target = date != null ? date : LocalDate.now();
        return bookingRepository.searchStaffBookings(
                        target.atStartOfDay(),
                        target.plusDays(1).atStartOfDay(),
                        StringUtils.hasText(status) ? status : null,
                        StringUtils.hasText(keyword) ? keyword.trim() : null)
                .stream()
                .map(this::toOrderMap)
                .toList();
    }

    public Map<String, Object> profile(Long staffId) {
        return toUserMap(findUser(staffId));
    }

    @Transactional
    public Map<String, Object> updateProfile(Long staffId, Map<String, Object> request) {
        User user = findUser(staffId);
        user.setFullName(stringValue(request.get("fullName")));
        user.setPhone(stringValue(request.get("phone")));
        user.setEmail(stringValue(request.get("email")));
        return toUserMap(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long staffId, Map<String, Object> request) {
        User user = findUser(staffId);
        String currentPassword = stringValue(request.get("currentPassword"));
        String newPassword = stringValue(request.get("newPassword"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private Invoice createInvoiceAndPayment(Booking booking, User staff, BigDecimal total, String method) {
        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElseGet(Invoice::new);
        invoice.setBooking(booking);
        invoice.setCustomer(booking.getCustomer());
        invoice.setShowtime(booking.getShowtime());
        invoice.setTotalAmount(total);
        invoice.setStatus("PAID");
        invoice.setCustomerUsername(booking.getCustomer().getUser() != null ? booking.getCustomer().getUser().getUsername() : booking.getCustomer().getPhone());
        invoice.setMembershipProcessed(invoice.getMembershipProcessed() != null ? invoice.getMembershipProcessed() : false);
        invoice.setStaff(staff);
        invoice = invoiceRepository.save(invoice);

        // Cộng điểm và cập nhật hạng membership
        membershipService.processInvoice(invoice);

        Payment payment = paymentRepository.findTopByInvoiceBookingIdOrderByCreatedAtDesc(booking.getId()).orElseGet(Payment::new);
        payment.setInvoice(invoice);
        payment.setPaymentMethod(method);
        payment.setTransactionCode(booking.getPaymentReference());
        payment.setAmount(total);
        payment.setOriginalAmount(total);
        payment.setPayableAmount(total);
        payment.setPaidAmount(total);
        payment.setStatus("PAID");
        payment.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return invoice;
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

    private void markSeatsBooked(Booking booking, Long staffId) {
        for (ShowtimeSeat seat : booking.getSeats()) {
            seat.setStatus("BOOKED");
            seat.setLockedAt(null);
            seat.setLockedBy(null);
        }
        showtimeSeatRepository.saveAll(booking.getSeats());
        seatEventPublisher.publish(booking.getShowtime().getId(), SeatEventDto.builder()
                .type(SeatEventDto.Type.SEAT_SOLD)
                .showtimeId(booking.getShowtime().getId())
                .seatIds(booking.getSeats().stream().map(seat -> seat.getSeat().getId()).toList())
                .byUserId(staffId)
                .build());
    }

    private void sendTicketEmailIfPossible(Booking booking, List<Ticket> tickets) {
        if (booking.getCustomer() == null || !StringUtils.hasText(booking.getCustomer().getEmail())) return;
        try {
            ticketEmailService.sendTicketEmail(booking, tickets);
            booking.setTicketEmailSent(true);
            bookingRepository.save(booking);
        } catch (Exception e) {
            log.warn("Ticket email failed for counter booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    private Map<String, Object> toCounterTicketMap(Ticket ticket, Booking booking) {
        ShowtimeSeat seat = ticket.getShowtimeSeat();
        Showtime showtime = seat != null ? seat.getShowtime() : booking.getShowtime();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ticketId", ticket.getId());
        map.put("id", ticket.getId());
        map.put("ticketCode", ticket.getQrCode());
        map.put("qrCode", ticket.getQrCode());
        map.put("qrPayload", ticket.getQrCode());
        map.put("checkInStatus", ticket.getStatus());
        map.put("status", ticket.getStatus());
        map.put("seatLabel", seatLabel(ticket));
        map.put("price", ticket.getPrice());
        map.put("bookingId", booking.getId());
        map.put("bookingCode", bookingCode(booking));
        map.put("movieTitle", showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Đang cập nhật");
        map.put("startTime", showtime != null ? showtime.getStartTime() : null);
        map.put("endTime", showtime != null ? showtime.getEndTime() : null);
        map.put("roomName", showtime != null && showtime.getRoom() != null ? showtime.getRoom().getName() : "Phòng chưa xác định");
        map.put("paymentMethod", booking.getPaymentMethod());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("ticketTotal", valueOrZero(booking.getTicketTotal()));
        map.put("comboTotal", valueOrZero(booking.getComboTotal()));
        map.put("grossTotal", valueOrZero(booking.getGrossTotal()));
        map.put("discountAmount", valueOrZero(booking.getDiscountAmount()));
        map.put("voucherDiscountAmount", valueOrZero(booking.getVoucherDiscountAmount()));
        map.put("membershipTierName", booking.getMembershipTierName());
        map.put("membershipDiscountPercent", valueOrZero(booking.getMembershipDiscountPercent()));
        map.put("membershipDiscountAmount", valueOrZero(booking.getMembershipDiscountAmount()));
        map.put("finalAmount", valueOrZero(booking.getFinalAmount()).compareTo(BigDecimal.ZERO) > 0 ? valueOrZero(booking.getFinalAmount()) : ticket.getPrice());
        map.put("totalAmount", valueOrZero(booking.getFinalAmount()).compareTo(BigDecimal.ZERO) > 0 ? valueOrZero(booking.getFinalAmount()) : ticket.getPrice());
        map.put("promotionCode", booking.getPromotionCode());
        return map;
    }

    private void validateCounterSeats(Showtime showtime, List<ShowtimeSeat> seats, List<Long> requestedIds, Long staffId) {
        if (seats.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Có ghế không hợp lệ.");
        }
        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : seats) {
            if (!showtime.getId().equals(seat.getShowtime().getId())) {
                throw new IllegalArgumentException("Tất cả ghế phải thuộc cùng suất chiếu.");
            }
            if (seat.getSeat() != null && "MAINTENANCE".equalsIgnoreCase(seat.getSeat().getStatus())) {
                throw new IllegalArgumentException("Ghế đang bảo trì, vui lòng chọn ghế khác.");
            }
            if ("BOOKED".equals(seat.getStatus())) {
                throw new IllegalArgumentException("Ghế đã bán: " + seatLabel(seat));
            }
            if (!"LOCKED".equals(seat.getStatus()) || seat.getLockedBy() == null || !seat.getLockedBy().equals(staffId)) {
                throw new IllegalArgumentException("Ghế phải được nhân viên hiện tại giữ trước khi bán: " + seatLabel(seat));
            }
            if (seat.getLockedAt() == null || seat.getLockedAt().plusMinutes(10).isBefore(now)) {
                throw new IllegalArgumentException("Ghế đã hết thời gian giữ: " + seatLabel(seat));
            }
        }
    }

    private Customer resolveCounterCustomer(Map<String, Object> request) {
        Long customerId = longValue(request.get("customerId"));
        if (customerId != null) {
            return customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng."));
        }
        Object info = request.get("customerInfo");
        if (info instanceof Map<?, ?> map) {
            return createOrFindCustomer(map.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue)));
        }
        Customer walkIn = new Customer();
        walkIn.setFullName("Khách vãng lai");
        walkIn.setTotalSpent(BigDecimal.ZERO);
        walkIn.setPoints(0);
        return customerRepository.save(walkIn);
    }

    private Map<String, Object> toShowtimeMap(Showtime showtime) {
        long booked = showtimeSeatRepository.countByShowtimeIdAndStatus(showtime.getId(), "BOOKED");
        long available = showtimeSeatRepository.countByShowtimeIdAndStatus(showtime.getId(), "AVAILABLE");
        String displayStatus = showtimeStatusService.displayStatus(showtime);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("showtimeId", showtime.getId());
        map.put("movieTitle", showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Đang cập nhật");
        map.put("poster", showtime.getMovie() != null ? showtime.getMovie().getPosterUrl() : null);
        map.put("room", showtime.getRoom() != null ? showtime.getRoom().getName() : "TBA");
        map.put("startTime", showtime.getStartTime());
        map.put("endTime", showtime.getEndTime());
        map.put("availableSeats", available);
        map.put("bookedSeats", booked);
        map.put("status", showtime.getStatus());
        map.put("statusLabel", showtimeStatusService.manualStatusLabel(showtime.getStatus()));
        map.put("displayStatus", displayStatus);
        map.put("displayStatusLabel", showtimeStatusService.displayStatusLabel(displayStatus));
        return map;
    }

    private Map<String, Object> toAttendanceMap(Attendance attendance) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", attendance.getId());
        map.put("date", attendance.getBusinessDate());
        map.put("staffId", attendance.getStaff().getId());
        map.put("staffName", attendance.getStaff().getFullName());
        map.put("shiftName", value(attendance.getShiftName()));
        map.put("assignmentId", attendance.getAssignmentId());
        map.put("assignmentTypeLabel", value(attendance.getShiftName()));
        map.put("shiftStart", value(attendance.getShiftStartTime()));
        map.put("shiftEnd", value(attendance.getShiftEndTime()));
        map.put("checkInTime", attendance.getCheckInTime());
        map.put("checkOutTime", attendance.getCheckOutTime());
        map.put("status", value(attendance.getStatus()));
        map.put("lateMinutes", attendance.getLateMinutes() == null ? 0 : attendance.getLateMinutes());
        map.put("earlyLeaveMinutes", attendance.getEarlyLeaveMinutes() == null ? 0 : attendance.getEarlyLeaveMinutes());
        map.put("workingMinutes", attendance.getWorkedMinutes() == null ? 0 : attendance.getWorkedMinutes());
        map.put("note", value(attendance.getNote()));
        addCheckInWindowFields(map, attendance.getAssignmentId(), parseShiftTime(attendance.getShiftStartTime(), DEFAULT_SHIFT_START), attendance.getCheckInTime(), attendance.getCheckOutTime());
        map.put("canCheckOut", attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null);
        addLocationFields(map, attendance);
        return map;
    }

    private Map<String, Object> emptyAttendance(User staff, LocalDate date) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", date);
        map.put("staffId", staff.getId());
        map.put("staffName", staff.getFullName());
        map.put("shiftName", "Chưa được phân ca");
        StaffAssignment assignment = staffSchedulingService.findTodayAssignment(staff.getId(), date);
        if (assignment != null) {
            map.put("assignmentId", assignment.getId());
            map.put("assignmentType", assignment.getAssignmentType().name());
            map.put("assignmentTypeLabel", assignment.getAssignmentType().getLabel());
            map.put("shiftName", assignment.getAssignmentType().getLabel());
            map.put("shiftStart", assignment.getStartTime().toString());
            map.put("shiftEnd", assignment.getEndTime().toString());
        } else {
            map.put("assignmentId", null);
            map.put("assignmentType", null);
            map.put("assignmentTypeLabel", null);
            map.put("shiftStart", null);
            map.put("shiftEnd", null);
        }
        map.put("checkInTime", null);
        map.put("checkOutTime", null);
        map.put("status", "NOT_CHECKED_IN");
        map.put("lateMinutes", 0);
        map.put("earlyLeaveMinutes", 0);
        map.put("workingMinutes", 0);
        map.put("note", "");
        LocalTime shiftStart = parseShiftTime(map.get("shiftStart") == null ? null : String.valueOf(map.get("shiftStart")), DEFAULT_SHIFT_START);
        Long assignmentId = map.get("assignmentId") instanceof Long value ? value : null;
        addCheckInWindowFields(map, assignmentId, shiftStart, null, null);
        map.put("canCheckOut", false);
        map.put("locationCheckEnabled", attendanceLocationCheckEnabled);
        map.put("workplaceName", attendanceLocationName);
        map.put("workplaceLatitude", attendanceLatitude);
        map.put("workplaceLongitude", attendanceLongitude);
        map.put("allowedRadiusMeters", attendanceAllowedRadiusMeters);
        map.put("maxAccuracyMeters", attendanceMaxAccuracyMeters);
        map.put("strictAccuracyCheck", strictAccuracyCheck);
        map.put("checkInLocationStatus", "NO_DATA");
        map.put("checkOutLocationStatus", "NO_DATA");
        return map;
    }

    private LocationValidation validateLocation(AttendanceLocationRequest request) {
        if (!attendanceLocationCheckEnabled) {
            log.info("Staff attendance location check is disabled.");
            return LocationValidation.disabled(request);
        }
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Không lấy được vị trí hiện tại.");
        }
        boolean lowAccuracy = request.getAccuracyMeters() != null
                && request.getAccuracyMeters().compareTo(attendanceMaxAccuracyMeters) > 0;
        if (lowAccuracy && strictAccuracyCheck) {
            throw new BadRequestException("Độ chính xác vị trí quá thấp, vui lòng thử lại.");
        }
        BigDecimal distance = locationService.calculateDistanceMeters(
                request.getLatitude(),
                request.getLongitude(),
                attendanceLatitude,
                attendanceLongitude
        );
        if (distance.compareTo(BigDecimal.valueOf(attendanceAllowedRadiusMeters)) > 0) {
            log.info(
                    "Staff attendance location rejected: receivedLat={}, receivedLng={}, centerLat={}, centerLng={}, distanceMeters={}, allowedRadiusMeters={}",
                    request.getLatitude(),
                    request.getLongitude(),
                    attendanceLatitude,
                    attendanceLongitude,
                    distance,
                    attendanceAllowedRadiusMeters
            );
            throw new BadRequestException("Bạn đang ở ngoài phạm vi chấm công. Vui lòng đến gần rạp hoặc liên hệ quản lý.");
        }
        return new LocationValidation(
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracyMeters(),
                distance,
                true,
                lowAccuracy ? "Độ chính xác vị trí thấp, kết quả chỉ dùng cho demo." : null
        );
    }

    private void applyCheckInLocation(Attendance attendance, LocationValidation location, String ipAddress, String userAgent) {
        applyWorkplace(attendance);
        attendance.setCheckInIpAddress(trim(ipAddress, 80));
        attendance.setCheckInUserAgent(trim(userAgent, 500));
        attendance.setCheckInLat(location.latitude());
        attendance.setCheckInLng(location.longitude());
        attendance.setCheckInAccuracyMeters(location.accuracyMeters());
        attendance.setCheckInDistanceMeters(location.distanceMeters());
        attendance.setCheckInLocationValid(location.valid());
        appendLocationWarning(attendance, location.warningMessage());
    }

    private void applyCheckOutLocation(Attendance attendance, LocationValidation location, String ipAddress, String userAgent) {
        applyWorkplace(attendance);
        attendance.setCheckOutIpAddress(trim(ipAddress, 80));
        attendance.setCheckOutUserAgent(trim(userAgent, 500));
        attendance.setCheckOutLat(location.latitude());
        attendance.setCheckOutLng(location.longitude());
        attendance.setCheckOutAccuracyMeters(location.accuracyMeters());
        attendance.setCheckOutDistanceMeters(location.distanceMeters());
        attendance.setCheckOutLocationValid(location.valid());
        appendLocationWarning(attendance, location.warningMessage());
    }

    private void applyWorkplace(Attendance attendance) {
        attendance.setWorkplaceName(attendanceLocationName);
        attendance.setWorkplaceLatitude(attendanceLatitude);
        attendance.setWorkplaceLongitude(attendanceLongitude);
        attendance.setAllowedRadiusMeters(attendanceAllowedRadiusMeters);
    }

    private void addLocationFields(Map<String, Object> map, Attendance attendance) {
        map.put("locationCheckEnabled", attendanceLocationCheckEnabled);
        map.put("workplaceName", value(attendance.getWorkplaceName() != null ? attendance.getWorkplaceName() : attendanceLocationName));
        map.put("workplaceLatitude", attendance.getWorkplaceLatitude() != null ? attendance.getWorkplaceLatitude() : attendanceLatitude);
        map.put("workplaceLongitude", attendance.getWorkplaceLongitude() != null ? attendance.getWorkplaceLongitude() : attendanceLongitude);
        map.put("allowedRadiusMeters", attendance.getAllowedRadiusMeters() != null ? attendance.getAllowedRadiusMeters() : attendanceAllowedRadiusMeters);
        map.put("maxAccuracyMeters", attendanceMaxAccuracyMeters);
        map.put("strictAccuracyCheck", strictAccuracyCheck);
        map.put("checkInLatitude", attendance.getCheckInLat());
        map.put("checkInLongitude", attendance.getCheckInLng());
        map.put("checkInAccuracyMeters", attendance.getCheckInAccuracyMeters());
        map.put("checkInDistanceMeters", attendance.getCheckInDistanceMeters());
        map.put("checkInLocationValid", attendance.getCheckInLocationValid());
        map.put("checkInLocationStatus", locationStatus(attendance.getCheckInLocationValid(), attendance.getCheckInDistanceMeters(), attendance.getCheckInAccuracyMeters()));
        map.put("checkInLocationWarning", locationWarning(attendance.getCheckInAccuracyMeters()));
        map.put("checkOutLatitude", attendance.getCheckOutLat());
        map.put("checkOutLongitude", attendance.getCheckOutLng());
        map.put("checkOutAccuracyMeters", attendance.getCheckOutAccuracyMeters());
        map.put("checkOutDistanceMeters", attendance.getCheckOutDistanceMeters());
        map.put("checkOutLocationValid", attendance.getCheckOutLocationValid());
        map.put("checkOutLocationStatus", locationStatus(attendance.getCheckOutLocationValid(), attendance.getCheckOutDistanceMeters(), attendance.getCheckOutAccuracyMeters()));
        map.put("checkOutLocationWarning", locationWarning(attendance.getCheckOutAccuracyMeters()));
    }

    private String locationStatus(Boolean valid, BigDecimal distance, BigDecimal accuracy) {
        if (valid == null && distance == null && accuracy == null) return "NO_DATA";
        if (Boolean.TRUE.equals(valid)) return "VALID";
        if (accuracy != null && accuracy.compareTo(attendanceMaxAccuracyMeters) > 0) return "LOW_ACCURACY";
        return "OUT_OF_RANGE";
    }

    private String locationWarning(BigDecimal accuracy) {
        if (!strictAccuracyCheck && accuracy != null && accuracy.compareTo(attendanceMaxAccuracyMeters) > 0) {
            return "Độ chính xác vị trí thấp, kết quả chỉ dùng cho demo.";
        }
        return null;
    }

    private void appendLocationWarning(Attendance attendance, String warning) {
        if (warning == null || warning.isBlank()) return;
        String note = attendance.getNote();
        if (note != null && note.contains(warning)) return;
        attendance.setNote(note == null || note.isBlank() ? warning : note + " " + warning);
    }

    private String trim(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record LocationValidation(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal accuracyMeters,
            BigDecimal distanceMeters,
            boolean valid,
            String warningMessage
    ) {
        private static LocationValidation disabled(AttendanceLocationRequest request) {
            return new LocationValidation(
                    request != null ? request.getLatitude() : null,
                    request != null ? request.getLongitude() : null,
                    request != null ? request.getAccuracyMeters() : null,
                    null,
                    true,
                    null
            );
        }
    }

    private Map<String, Object> toOrderMap(Booking booking) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bookingId", booking.getId());
        map.put("customer", booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Khách vãng lai");
        map.put("phone", booking.getCustomer() != null ? booking.getCustomer().getPhone() : "");
        map.put("email", booking.getCustomer() != null ? booking.getCustomer().getEmail() : "");
        map.put("movie", booking.getShowtime() != null && booking.getShowtime().getMovie() != null ? booking.getShowtime().getMovie().getTitle() : "");
        map.put("showtime", booking.getShowtime() != null ? booking.getShowtime().getStartTime() : null);
        map.put("room", booking.getShowtime() != null && booking.getShowtime().getRoom() != null ? booking.getShowtime().getRoom().getName() : "");
        map.put("seats", booking.getSeats().stream().map(this::seatLabel).sorted().toList());
        map.put("combos", comboService.bookingComboResponses(booking.getId()));
        bookingPricingService.ensurePricingSnapshot(booking);
        map.put("totalAmount", booking.getFinalAmount());
        map.put("paymentMethod", booking.getPaymentMethod());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("bookingStatus", booking.getStatus());
        map.put("createdAt", booking.getCreatedAt());
        return map;
    }

    private Map<String, Object> toFoodOrderMap(FoodOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", order.getId());
        map.put("customer", value(order.getCustomerName()));
        map.put("phone", value(order.getCustomerPhone()));
        map.put("staffName", order.getStaff() != null ? order.getStaff().getFullName() : "");
        map.put("totalAmount", order.getTotalAmount());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("status", order.getStatus());
        map.put("createdAt", order.getCreatedAt());
        map.put("items", order.getItems().stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("comboId", item.getCombo() != null ? item.getCombo().getId() : null);
            row.put("comboName", item.getComboNameSnapshot());
            row.put("unitPrice", item.getUnitPriceSnapshot());
            row.put("quantity", item.getQuantity());
            row.put("subtotal", item.getSubtotal());
            return row;
        }).toList());
        return map;
    }

    private List<ComboItemRequest> comboRequests(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<ComboItemRequest> items = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) continue;
            Long comboId = longValue(map.get("comboId"));
            Integer quantity = intValue(map.get("quantity"));
            if (comboId == null || quantity == null || quantity <= 0) continue;
            ComboItemRequest item = new ComboItemRequest();
            item.setComboId(comboId);
            item.setQuantity(quantity);
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> toCustomerMap(Customer customer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", customer.getId());
        map.put("fullName", value(customer.getFullName()));
        map.put("phone", value(customer.getPhone()));
        map.put("email", value(customer.getEmail()));
        return map;
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("position", user.getStaffPosition());
        map.put("positionLabel", positionLabel(user.getStaffPosition()));
        map.put("contractType", user.getEmploymentType());
        map.put("contractTypeLabel", contractTypeLabel(user.getEmploymentType()));
        map.put("roles", user.getRoles().stream().map(Role::getName).toList());
        return map;
    }

    public void requirePositionOrAdmin(UserDetailsImpl currentUser, StaffPosition requiredPosition) {
        boolean admin = currentUser.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) return;
        User user = findUser(currentUser.getId());
        if (!requiredPosition.name().equals(user.getStaffPosition())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, positionDeniedMessage(user.getStaffPosition(), requiredPosition));
        }
    }

    public void denyTicketCheckerSalesAccess(UserDetailsImpl currentUser) {
        if (currentUser == null) return;
        boolean staff = currentUser.getAuthorities().stream().anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));
        boolean admin = currentUser.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!staff || admin) return;
        User user = findUser(currentUser.getId());
        if (StaffPosition.TICKET_CHECKER.name().equals(user.getStaffPosition())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên soát vé không có quyền bán vé tại quầy.");
        }
    }

    private String positionDeniedMessage(String currentPosition, StaffPosition requiredPosition) {
        if (requiredPosition == StaffPosition.COUNTER_SALES && StaffPosition.TICKET_CHECKER.name().equals(currentPosition)) {
            return "Nhân viên soát vé không có quyền bán vé tại quầy.";
        }
        if (requiredPosition == StaffPosition.TICKET_CHECKER && StaffPosition.COUNTER_SALES.name().equals(currentPosition)) {
            return "Nhân viên bán tại quầy không có quyền soát vé.";
        }
        return "Bạn không có quyền truy cập chức năng này.";
    }

    private void applyAssignmentShift(Attendance attendance, StaffAssignment assignment) {
        attendance.setAssignmentId(assignment.getId());
        attendance.setShiftName(assignment.getAssignmentType().getLabel());
        attendance.setShiftStartTime(assignment.getStartTime().toString());
        attendance.setShiftEndTime(assignment.getEndTime().toString());
    }

    private void addCheckInWindowFields(Map<String, Object> map, Long assignmentId, LocalTime shiftStart, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        LocalTime availableFrom = checkInAvailableFrom(shiftStart);
        boolean hasAssignment = assignmentId != null;
        boolean tooEarly = hasAssignment && LocalTime.now().isBefore(availableFrom);
        String blockedReason = !hasAssignment
                ? "Bạn chưa được phân ca hôm nay. Vui lòng liên hệ quản lý."
                : tooEarly ? "Bạn có thể check-in từ " + availableFrom + "." : "";
        map.put("hasAssignment", hasAssignment);
        map.put("checkInAvailableFrom", assignmentId == null ? null : availableFrom.toString());
        map.put("checkInBlockedReason", blockedReason);
        map.put("canCheckIn", hasAssignment && checkInTime == null && checkOutTime == null && !tooEarly);
    }

    private LocalTime checkInAvailableFrom(LocalTime shiftStart) {
        return shiftStart.minusMinutes(earlyCheckInMinutes == null ? 15 : earlyCheckInMinutes);
    }

    private boolean isLate(LocalTime now, LocalTime shiftStart) {
        return now.isAfter(shiftStart.plusMinutes(lateGraceMinutes == null ? 5 : lateGraceMinutes));
    }

    private LocalTime parseShiftTime(String value, LocalTime fallback) {
        try {
            return StringUtils.hasText(value) ? LocalTime.parse(value) : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String positionLabel(String value) {
        try {
            return StaffPosition.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String contractTypeLabel(String value) {
        try {
            return ContractType.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String seatLabel(ShowtimeSeat seat) {
        return seat.getSeat() == null ? String.valueOf(seat.getId()) : seat.getSeat().getRowLabel() + seat.getSeat().getColumnNumber();
    }

    private String seatLabel(Ticket ticket) {
        return ticket.getShowtimeSeat() == null ? String.valueOf(ticket.getId()) : seatLabel(ticket.getShowtimeSeat());
    }

    private String bookingCode(Booking booking) {
        return "BK-" + booking.getId();
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
    }

    private Long longValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private Integer intValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        return Integer.valueOf(String.valueOf(value));
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(this::longValue).filter(Objects::nonNull).toList();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
