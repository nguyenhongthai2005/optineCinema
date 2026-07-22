package com.opticine.service;

import com.opticine.dto.admin.customer.CustomerBookingResponse;
import com.opticine.dto.admin.customer.CustomerDetailResponse;
import com.opticine.dto.admin.customer.CustomerResponse;
import com.opticine.entity.*;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.InvoiceRepository;
import com.opticine.repository.PaymentRepository;
import com.opticine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminCustomerService {
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingPricingService bookingPricingService;
    private final MembershipService membershipService;

    public AdminCustomerService(CustomerRepository customerRepository, BookingRepository bookingRepository,
                                PaymentRepository paymentRepository, UserRepository userRepository,
                                InvoiceRepository invoiceRepository, BookingPricingService bookingPricingService,
                                MembershipService membershipService) {
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.bookingPricingService = bookingPricingService;
        this.membershipService = membershipService;
    }

    public List<CustomerResponse> search(String keyword) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return customerRepository.searchCustomers(normalized).stream().map(this::toResponse).toList();
    }

    public CustomerDetailResponse getDetail(Long id) {
        Customer customer = findCustomer(id);
        String membershipName = customer.getMembership() != null ? customer.getMembership().getName() : null;
        return CustomerDetailResponse.builder()
                .profile(toResponse(customer))
                .membershipName(membershipName)
                .build();
    }

    public List<CustomerBookingResponse> getBookings(Long id) {
        Customer customer = findCustomer(id);
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, String status) {
        Customer customer = findCustomer(id);
        User user = customer.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Customer has no linked user account");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        user.setStatus(normalized);
        user.setEnabled("ACTIVE".equals(normalized));
        userRepository.save(user);
        return toResponse(customer);
    }

    @Transactional
    public java.util.Map<String, Object> recalculateSpending() {
        int bookingsUpdated = 0;
        for (Booking booking : bookingRepository.findSuccessfulPaidBookings()) {
            boolean missing = booking.getTicketTotal() == null
                    || booking.getComboTotal() == null
                    || booking.getGrossTotal() == null
                    || booking.getDiscountAmount() == null
                    || booking.getFinalAmount() == null;
            bookingPricingService.ensurePricingSnapshot(booking, false);
            if (missing) {
                bookingRepository.save(booking);
                bookingsUpdated++;
            }
            invoiceRepository.findByBookingId(booking.getId())
                    .filter(invoice -> "PAID".equalsIgnoreCase(invoice.getStatus()))
                    .ifPresent(invoice -> {
                        if (invoice.getTotalAmount() == null) {
                            invoice.setTotalAmount(booking.getFinalAmount());
                            invoiceRepository.save(invoice);
                        }
                    });
        }

        int customersUpdated = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Customer customer : customerRepository.findAll()) {
            BigDecimal totalSpent = calculateTotalSpent(customer);
            totalRevenue = totalRevenue.add(totalSpent);
            BigDecimal currentSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
            Integer currentPoints = customer.getPoints() != null ? customer.getPoints() : 0;
            Long oldMembershipId = customer.getMembership() != null ? customer.getMembership().getId() : null;
            membershipService.setCustomerSpending(customer, totalSpent);
            Long newMembershipId = customer.getMembership() != null ? customer.getMembership().getId() : null;
            if (currentSpent.compareTo(totalSpent) != 0
                    || !currentPoints.equals(customer.getPoints() != null ? customer.getPoints() : 0)
                    || !java.util.Objects.equals(oldMembershipId, newMembershipId)) {
                customersUpdated++;
            }
        }

        return java.util.Map.of(
                "customersUpdated", customersUpdated,
                "bookingsUpdated", bookingsUpdated,
                "totalRevenueRecalculated", totalRevenue,
                "message", "Đã đồng bộ lại tổng chi tiêu và membership."
        );
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private CustomerResponse toResponse(Customer customer) {
        User user = customer.getUser();
        BigDecimal totalSpent = calculateTotalSpent(customer);
        
        return CustomerResponse.builder()
                .id(customer.getId())
                .userId(user != null ? user.getId() : null)
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .status(user != null ? user.getStatus() : null)
                .enabled(user != null ? user.getEnabled() : null)
                .createdAt(user != null ? user.getCreatedAt() : null)
                .totalBookings(bookingRepository.countByCustomerId(customer.getId()))
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .latestBookingDate(bookingRepository.findLatestBookingDate(customer.getId()).orElse(null))
                .points(customer.getPoints())
                .build();
    }

    private CustomerBookingResponse toBookingResponse(Booking booking) {
        Showtime showtime = booking.getShowtime();
        String movie = showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Dang cap nhat";
        List<String> seats = booking.getSeats() == null ? List.of() : booking.getSeats().stream()
                .map(showtimeSeat -> {
                    Seat seat = showtimeSeat.getSeat();
                    return seat == null ? String.valueOf(showtimeSeat.getId()) : seat.getRowLabel() + seat.getColumnNumber();
                })
                .toList();
        BigDecimal total = calculateBookingTotal(booking);
        String paymentStatus = showtime == null ? null : paymentRepository
                .findTopByInvoiceCustomerIdAndInvoiceShowtimeIdOrderByCreatedAtDesc(booking.getCustomer().getId(), showtime.getId())
                .map(Payment::getStatus)
                .orElse(null);

        return CustomerBookingResponse.builder()
                .id(booking.getId())
                .movie(movie)
                .showtime(showtime != null ? showtime.getStartTime() : null)
                .seats(seats)
                .totalAmount(total)
                .paymentStatus(paymentStatus)
                .bookingStatus(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BigDecimal calculateBookingTotal(Booking booking) {
        bookingPricingService.ensurePricingSnapshot(booking);
        return booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalSpent(Customer customer) {
        List<Invoice> paidInvoices = invoiceRepository.findPaidInvoicesByCustomerId(customer.getId());
        BigDecimal invoiceTotal = paidInvoices.stream()
                .map(invoice -> invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<Long> invoicedBookingIds = paidInvoices.stream()
                .filter(invoice -> invoice.getBooking() != null)
                .map(invoice -> invoice.getBooking().getId())
                .collect(Collectors.toSet());
        BigDecimal bookingFallbackTotal = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(this::isSuccessfulPaid)
                .filter(booking -> booking.getId() == null || !invoicedBookingIds.contains(booking.getId()))
                .map(this::calculateBookingTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return invoiceTotal.add(bookingFallbackTotal);
    }

    private boolean isSuccessfulPaid(Booking booking) {
        return "CONFIRMED".equalsIgnoreCase(booking.getStatus())
                && "PAID".equalsIgnoreCase(booking.getPaymentStatus());
    }
}
