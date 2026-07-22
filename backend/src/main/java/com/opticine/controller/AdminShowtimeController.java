package com.opticine.controller;

import com.opticine.dto.admin.showtime.AdminShowtimeRequest;
import com.opticine.dto.admin.showtime.AdminShowtimeStatusRequest;
import com.opticine.service.AdminShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/showtimes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final AdminShowtimeService adminShowtimeService;

    /**
     * GET /api/admin/showtimes?movieId=1&date=2026-06-20
     * Trả về tất cả suất chiếu (kể cả CANCELLED).
     */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminShowtimeService.getAll(movieId, date));
    }

    /** GET /api/admin/showtimes/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminShowtimeService.getById(id));
    }

    /**
     * POST /api/admin/showtimes
     * Tạo suất chiếu mới + tự động sinh showtime_seats từ ghế trong phòng.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AdminShowtimeRequest request) {
        return ResponseEntity.ok(adminShowtimeService.create(request));
    }

    /** PUT /api/admin/showtimes/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminShowtimeRequest request) {
        return ResponseEntity.ok(adminShowtimeService.update(id, request));
    }

    /** PATCH /api/admin/showtimes/{id}/status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody AdminShowtimeStatusRequest request) {
        return ResponseEntity.ok(adminShowtimeService.updateStatus(id, request.getStatus()));
    }
}
