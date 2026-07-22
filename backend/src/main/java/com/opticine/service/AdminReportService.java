package com.opticine.service;

import com.opticine.dto.admin.analytics.BookingReportItem;
import com.opticine.dto.admin.analytics.RevenueTimelineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AdminAnalyticsService analyticsService;

    public byte[] bookingCsv(LocalDate from, LocalDate to, Long movieId, String paymentMethod, String bookingStatus) {
        List<BookingReportItem> rows = analyticsService.bookingReport(from, to, movieId, paymentMethod, bookingStatus);
        StringBuilder csv = new StringBuilder("\uFEFF");
        row(csv, "Booking ID", "Created at", "Paid at", "Customer", "Email", "Phone", "Movie", "Room",
                "Showtime", "Seats", "Combos", "Tickets", "Invoice amount", "Paid amount", "Payment method",
                "Booking status", "Payment status", "Transaction code");
        for (BookingReportItem item : rows) {
            row(csv, item.getBookingId(), time(item.getCreatedAt()), time(item.getPaidAt()), item.getCustomerName(),
                    item.getCustomerEmail(), item.getCustomerPhone(), item.getMovieTitle(), item.getRoomName(),
                    time(item.getShowtimeStart()), item.getSeats(), item.getCombos(), item.getTicketCount(),
                    item.getInvoiceAmount(), item.getPaidAmount(), item.getPaymentMethod(), item.getBookingStatus(),
                    item.getPaymentStatus(), item.getTransactionCode());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] revenueCsv(LocalDate from, LocalDate to, Long movieId, String paymentMethod, String bookingStatus) {
        List<RevenueTimelineItem> rows = analyticsService.revenueReport(from, to, movieId, paymentMethod, bookingStatus);
        StringBuilder csv = new StringBuilder("\uFEFF");
        row(csv, "Date", "Total bookings", "Paid bookings", "Tickets sold", "Order revenue", "Actual paid");
        for (RevenueTimelineItem item : rows) {
            row(csv, item.getDate(), item.getTotalBookings(), item.getPaidBookings(), item.getTicketsSold(),
                    item.getRevenue(), item.getActualPaidRevenue());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String time(LocalDateTime value) { return value == null ? "" : value.format(DATE_TIME); }

    private void row(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(escape(values[i]));
        }
        csv.append("\r\n");
    }

    private String escape(Object raw) {
        String value = raw == null ? "" : String.valueOf(raw);
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
