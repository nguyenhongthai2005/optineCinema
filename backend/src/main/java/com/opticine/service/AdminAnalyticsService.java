package com.opticine.service;

import com.opticine.dto.admin.analytics.*;
import com.opticine.entity.*;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public AnalyticsOverviewResponse overview(LocalDate fromDate, LocalDate toDate, Long movieId,
                                              String paymentMethod, String bookingStatus) {
        DateRange range = range(fromDate, toDate);
        List<Booking> bookings = filterBookings(bookingRepository.findReportBookings(range.from(), range.to()),
                movieId, paymentMethod, bookingStatus);
        List<Payment> payments = filterPayments(paymentRepository.findPaidBookingPayments(range.from(), range.to()),
                movieId, paymentMethod, bookingStatus);

        long days = ChronoUnit.DAYS.between(range.from().toLocalDate(), range.to().toLocalDate());
        LocalDateTime previousFrom = range.from().minusDays(days);
        List<Payment> previousPayments = filterPayments(
                paymentRepository.findPaidBookingPayments(previousFrom, range.from()), movieId, paymentMethod, bookingStatus);

        BigDecimal revenue = payments.stream().map(this::orderRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualPaidRevenue = payments.stream().map(this::actualPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousRevenue = previousPayments.stream().map(this::orderRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiceRevenue = payments.stream().map(this::invoiceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<Long> paidBookingIds = payments.stream().map(this::bookingId).filter(Objects::nonNull).collect(Collectors.toSet());
        long paidBookings = paidBookingIds.size();
        long totalBookings = bookings.size();
        long pendingBookings = bookings.stream().filter(this::isPending).count();
        long cancelledBookings = bookings.stream().filter(this::isCancelled).count();
        long ticketsSold = payments.stream().map(Payment::getInvoice).filter(Objects::nonNull)
                .map(Invoice::getBooking).filter(Objects::nonNull).filter(distinctById())
                .mapToLong(this::seatCount).sum();

        return AnalyticsOverviewResponse.builder()
                .revenue(revenue)
                .orderRevenue(revenue)
                .actualPaidRevenue(actualPaidRevenue)
                .demoDifference(revenue.subtract(actualPaidRevenue))
                .invoiceRevenue(invoiceRevenue)
                .averageBookingValue(paidBookings == 0 ? BigDecimal.ZERO
                        : revenue.divide(BigDecimal.valueOf(paidBookings), 2, RoundingMode.HALF_UP))
                .previousRevenue(previousRevenue)
                .revenueChangePercent(changePercent(revenue, previousRevenue))
                .totalBookings(totalBookings)
                .paidBookings(paidBookings)
                .pendingBookings(pendingBookings)
                .cancelledBookings(cancelledBookings)
                .ticketsSold(ticketsSold)
                .conversionRate(totalBookings == 0 ? 0D : round(100D * paidBookings / totalBookings))
                .timeline(timeline(range, bookings, payments))
                .topMovies(topMovies(payments))
                .paymentMethods(paymentMethods(payments))
                .recentBookings(reportRows(bookings).stream().limit(10).toList())
                .build();
    }

    public List<BookingReportItem> bookingReport(LocalDate fromDate, LocalDate toDate, Long movieId,
                                                 String paymentMethod, String bookingStatus) {
        DateRange range = range(fromDate, toDate);
        return reportRows(filterBookings(bookingRepository.findReportBookings(range.from(), range.to()),
                movieId, paymentMethod, bookingStatus));
    }

    public List<RevenueTimelineItem> revenueReport(LocalDate fromDate, LocalDate toDate, Long movieId,
                                                   String paymentMethod, String bookingStatus) {
        DateRange range = range(fromDate, toDate);
        List<Booking> bookings = filterBookings(bookingRepository.findReportBookings(range.from(), range.to()),
                movieId, paymentMethod, bookingStatus);
        List<Payment> payments = filterPayments(paymentRepository.findPaidBookingPayments(range.from(), range.to()),
                movieId, paymentMethod, bookingStatus);
        return timeline(range, bookings, payments);
    }

    private List<BookingReportItem> reportRows(List<Booking> bookings) {
        if (bookings.isEmpty()) return List.of();
        List<Long> ids = bookings.stream().map(Booking::getId).toList();
        Map<Long, Payment> latestPayment = new LinkedHashMap<>();
        for (Payment payment : paymentRepository.findByInvoiceBookingIdInOrderByCreatedAtDesc(ids)) {
            Long id = bookingId(payment);
            if (id != null) latestPayment.putIfAbsent(id, payment);
        }
        return bookings.stream().map(booking -> toReportItem(booking, latestPayment.get(booking.getId()))).toList();
    }

    private BookingReportItem toReportItem(Booking booking, Payment payment) {
        Showtime showtime = booking.getShowtime();
        Customer customer = booking.getCustomer();
        Invoice invoice = payment == null ? null : payment.getInvoice();
        String seats = booking.getSeats() == null ? "" : booking.getSeats().stream()
                .filter(s -> s.getSeat() != null)
                .sorted(Comparator.comparing((ShowtimeSeat s) -> s.getSeat().getRowLabel())
                        .thenComparing(s -> s.getSeat().getColumnNumber()))
                .map(s -> s.getSeat().getRowLabel() + s.getSeat().getColumnNumber())
                .collect(Collectors.joining(", "));
        String combos = booking.getComboItems() == null ? "" : booking.getComboItems().stream()
                .map(item -> item.getComboNameSnapshot() + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));
        return BookingReportItem.builder()
                .bookingId(booking.getId()).createdAt(booking.getCreatedAt()).paidAt(booking.getPaidAt())
                .customerName(customer == null ? "" : customer.getFullName())
                .customerEmail(customer == null ? "" : customer.getEmail())
                .customerPhone(customer == null ? "" : customer.getPhone())
                .movieId(movieId(booking)).movieTitle(movieTitle(booking))
                .roomName(showtime == null || showtime.getRoom() == null ? "" : showtime.getRoom().getName())
                .showtimeStart(showtime == null ? null : showtime.getStartTime())
                .seats(seats).combos(combos).ticketCount(seatCount(booking))
                .invoiceAmount(invoice == null ? BigDecimal.ZERO : zero(invoice.getTotalAmount()))
                .paidAmount(payment == null || !"PAID".equalsIgnoreCase(payment.getStatus()) ? BigDecimal.ZERO : actualPaid(payment))
                .paymentMethod(payment == null ? booking.getPaymentMethod() : payment.getPaymentMethod())
                .bookingStatus(booking.getStatus()).paymentStatus(booking.getPaymentStatus())
                .transactionCode(payment == null ? booking.getPaymentReference() : payment.getTransactionCode())
                .build();
    }

    private List<RevenueTimelineItem> timeline(DateRange range, List<Booking> bookings, List<Payment> payments) {
        Map<LocalDate, List<Booking>> bookingsByDate = bookings.stream().filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate()));
        Map<LocalDate, List<Payment>> paymentsByDate = payments.stream().filter(p -> paymentTime(p) != null)
                .collect(Collectors.groupingBy(p -> paymentTime(p).toLocalDate()));
        List<RevenueTimelineItem> result = new ArrayList<>();
        for (LocalDate date = range.from().toLocalDate(); date.isBefore(range.to().toLocalDate()); date = date.plusDays(1)) {
            List<Payment> dayPayments = paymentsByDate.getOrDefault(date, List.of());
            Set<Long> paidIds = dayPayments.stream().map(this::bookingId).filter(Objects::nonNull).collect(Collectors.toSet());
            long tickets = dayPayments.stream().map(Payment::getInvoice).filter(Objects::nonNull)
                    .map(Invoice::getBooking).filter(Objects::nonNull).filter(distinctById()).mapToLong(this::seatCount).sum();
            result.add(RevenueTimelineItem.builder().date(date)
                    .revenue(dayPayments.stream().map(this::orderRevenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .actualPaidRevenue(dayPayments.stream().map(this::actualPaid).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .paidBookings((long) paidIds.size()).totalBookings((long) bookingsByDate.getOrDefault(date, List.of()).size())
                    .ticketsSold(tickets).build());
        }
        return result;
    }

    private List<MovieAnalyticsItem> topMovies(List<Payment> payments) {
        Map<Long, MovieAccumulator> values = new HashMap<>();
        for (Payment payment : payments) {
            Booking booking = booking(payment);
            Long id = movieId(booking);
            if (id == null) continue;
            MovieAccumulator value = values.computeIfAbsent(id, ignored -> new MovieAccumulator(id, movieTitle(booking)));
            value.revenue = value.revenue.add(orderRevenue(payment));
            value.bookingIds.add(booking.getId());
            value.tickets += seatCount(booking);
        }
        return values.values().stream().sorted(Comparator.comparing((MovieAccumulator v) -> v.revenue).reversed())
                .limit(10).map(v -> MovieAnalyticsItem.builder().movieId(v.id).movieTitle(v.title).revenue(v.revenue)
                        .bookings((long) v.bookingIds.size()).tickets(v.tickets).build()).toList();
    }

    private List<PaymentMethodAnalyticsItem> paymentMethods(List<Payment> payments) {
        Map<String, List<Payment>> grouped = payments.stream().collect(Collectors.groupingBy(p -> normalize(p.getPaymentMethod(), "UNKNOWN")));
        return grouped.entrySet().stream().map(entry -> PaymentMethodAnalyticsItem.builder()
                        .paymentMethod(entry.getKey()).transactions((long) entry.getValue().size())
                        .revenue(entry.getValue().stream().map(this::orderRevenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                        .actualPaidRevenue(entry.getValue().stream().map(this::actualPaid).reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted(Comparator.comparing(PaymentMethodAnalyticsItem::getRevenue).reversed()).toList();
    }

    private List<Booking> filterBookings(List<Booking> values, Long movieId, String method, String status) {
        return values.stream().filter(b -> movieId == null || Objects.equals(movieId(b), movieId))
                .filter(b -> !StringUtils.hasText(method) || method.equalsIgnoreCase(b.getPaymentMethod()))
                .filter(b -> !StringUtils.hasText(status) || status.equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    private List<Payment> filterPayments(List<Payment> values, Long movieId, String method, String status) {
        return values.stream().filter(p -> movieId == null || Objects.equals(movieId(booking(p)), movieId))
                .filter(p -> !StringUtils.hasText(method) || method.equalsIgnoreCase(p.getPaymentMethod()))
                .filter(p -> !StringUtils.hasText(status) || (booking(p) != null && status.equalsIgnoreCase(booking(p).getStatus())))
                .toList();
    }

    private DateRange range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("from must be on or before to");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw new IllegalArgumentException("Date range cannot exceed 366 days");
        return new DateRange(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private BigDecimal orderRevenue(Payment p) {
        Invoice invoice = p.getInvoice();
        Booking booking = invoice != null ? invoice.getBooking() : null;

        if (invoice != null && invoice.getTotalAmount() != null) return invoice.getTotalAmount();
        if (booking != null && booking.getFinalAmount() != null) return booking.getFinalAmount();
        if (p.getOriginalAmount() != null) return p.getOriginalAmount();
        return zero(p.getAmount());
    }

    private BigDecimal actualPaid(Payment p) {
        if (p.getPaidAmount() != null) return p.getPaidAmount();
        if (p.getPayableAmount() != null) return p.getPayableAmount();
        return zero(p.getAmount());
    }
    private BigDecimal invoiceAmount(Payment p) { return p.getInvoice() == null ? BigDecimal.ZERO : zero(p.getInvoice().getTotalAmount()); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private Booking booking(Payment p) { return p.getInvoice() == null ? null : p.getInvoice().getBooking(); }
    private Long bookingId(Payment p) { Booking b = booking(p); return b == null ? null : b.getId(); }
    private LocalDateTime paymentTime(Payment p) { Booking b = booking(p); return p.getPaidAt() != null ? p.getPaidAt() : b == null ? null : b.getPaidAt(); }
    private Long movieId(Booking b) { return b == null || b.getShowtime() == null || b.getShowtime().getMovie() == null ? null : b.getShowtime().getMovie().getId(); }
    private String movieTitle(Booking b) { return b == null || b.getShowtime() == null || b.getShowtime().getMovie() == null ? "Unknown" : b.getShowtime().getMovie().getTitle(); }
    private long seatCount(Booking b) { return b == null || b.getSeats() == null ? 0 : b.getSeats().size(); }
    private boolean isPending(Booking b) { return Set.of("PENDING_PAYMENT", "WAITING_CONFIRMATION").contains(normalize(b.getStatus(), "")); }
    private boolean isCancelled(Booking b) { return "CANCELLED".equalsIgnoreCase(b.getStatus()) || "FAILED".equalsIgnoreCase(b.getPaymentStatus()); }
    private String normalize(String value, String fallback) { return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback; }
    private Double changePercent(BigDecimal current, BigDecimal previous) { return previous.signum() == 0 ? (current.signum() == 0 ? 0D : 100D) : round(current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP).doubleValue()); }
    private Double round(double value) { return Math.round(value * 100D) / 100D; }
    private <T extends Booking> java.util.function.Predicate<T> distinctById() { Set<Long> ids = new HashSet<>(); return b -> b.getId() != null && ids.add(b.getId()); }

    private record DateRange(LocalDateTime from, LocalDateTime to) {}
    private static class MovieAccumulator {
        private final Long id; private final String title; private BigDecimal revenue = BigDecimal.ZERO;
        private final Set<Long> bookingIds = new HashSet<>(); private long tickets;
        private MovieAccumulator(Long id, String title) { this.id = id; this.title = title; }
    }
}
