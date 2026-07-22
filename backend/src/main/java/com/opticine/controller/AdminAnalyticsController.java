package com.opticine.controller;

import com.opticine.dto.admin.analytics.AnalyticsOverviewResponse;
import com.opticine.dto.admin.analytics.BookingReportItem;
import com.opticine.service.AdminAnalyticsService;
import com.opticine.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAnalyticsController {
    private final AdminAnalyticsService analyticsService;
    private final AdminReportService reportService;

    @GetMapping("/analytics/overview")
    public AnalyticsOverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String bookingStatus) {
        return analyticsService.overview(from, to, movieId, paymentMethod, bookingStatus);
    }

    @GetMapping("/reports/bookings")
    public List<BookingReportItem> bookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String bookingStatus) {
        return analyticsService.bookingReport(from, to, movieId, paymentMethod, bookingStatus);
    }

    @GetMapping("/reports/bookings/export")
    public ResponseEntity<byte[]> exportBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String bookingStatus) {
        return csv("opticine-bookings-" + suffix(from, to) + ".csv",
                reportService.bookingCsv(from, to, movieId, paymentMethod, bookingStatus));
    }

    @GetMapping("/reports/revenue/export")
    public ResponseEntity<byte[]> exportRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String bookingStatus) {
        return csv("opticine-revenue-" + suffix(from, to) + ".csv",
                reportService.revenueCsv(from, to, movieId, paymentMethod, bookingStatus));
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }

    private String suffix(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        return start + "_" + end;
    }
}
