package com.opticine.controller;

import com.opticine.dto.showtime.response.SeatStatusResponse;
import com.opticine.dto.showtime.response.ShowtimeResponse;
import com.opticine.service.ShowtimeService;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * GET /api/showtimes?movieId=1&date=2026-06-15
     * Public — không cần đăng nhập
     */
    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getShowtimes(
            @RequestParam(value = "movieId", required = false) Long movieId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(showtimeService.getShowtimes(movieId, date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtime(@PathVariable("id") Long id) {
        return ResponseEntity.ok(showtimeService.getShowtime(id));
    }

    /**
     * GET /api/showtimes/{id}/seats
     * Public — không cần đăng nhập
     */
    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatStatusResponse>> getSeats(@PathVariable("id") Long id,
                                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(showtimeService.getSeatsForShowtime(id, userId));
    }
}
