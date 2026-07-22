package com.opticine.service;

import com.opticine.entity.Movie;
import com.opticine.entity.Room;
import com.opticine.entity.Showtime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
public class ShowtimeAvailabilityService {

    private static final Set<String> BOOKABLE_SHOWTIME_STATUSES = Set.of("OPEN", "ACTIVE", "SCHEDULED");
    private static final Set<String> BOOKABLE_MOVIE_STATUSES = Set.of("NOW_SHOWING", "ACTIVE");
    private static final Set<String> ACTIVE_ROOM_STATUSES = Set.of("ACTIVE");

    @Value("${app.booking.close-before-showtime-minutes:0}")
    private long closeBeforeShowtimeMinutes;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    public boolean isBookable(Showtime showtime) {
        if (showtime == null || showtime.getStartTime() == null) return false;
        if (!BOOKABLE_SHOWTIME_STATUSES.contains(normalize(showtime.getStatus()))) return false;

        Movie movie = showtime.getMovie();
        if (movie == null || !BOOKABLE_MOVIE_STATUSES.contains(normalize(movie.getStatus()))) return false;

        Room room = showtime.getRoom();
        if (room == null || !ACTIVE_ROOM_STATUSES.contains(normalize(room.getStatus()))) return false;

        LocalDateTime bookingClosesAt = showtime.getStartTime().minusMinutes(closeBeforeShowtimeMinutes);
        return now().isBefore(bookingClosesAt);
    }

    public void requireBookableForSeatLock(Showtime showtime) {
        if (!isBookable(showtime)) {
            throw new IllegalArgumentException("Suất chiếu đã bắt đầu hoặc đã hết thời gian đặt vé.");
        }
    }

    public void requireBookableForBooking(Showtime showtime) {
        if (!isBookable(showtime)) {
            throw new IllegalArgumentException("Suất chiếu đã hết thời gian đặt vé.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(timezone));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
