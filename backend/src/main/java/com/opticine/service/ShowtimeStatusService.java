package com.opticine.service;

import com.opticine.entity.Showtime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

@Service
public class ShowtimeStatusService {

    private static final Set<String> SCHEDULED_ALIASES = Set.of("SCHEDULED", "OPEN", "ACTIVE");

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    public String displayStatus(Showtime showtime) {
        return displayStatus(showtime, now());
    }

    public String displayStatus(Showtime showtime, LocalDateTime now) {
        if (showtime == null) return "ENDED";
        if ("CANCELLED".equals(normalize(showtime.getStatus()))) return "CANCELLED";
        if (showtime.getStartTime() != null && now.isBefore(showtime.getStartTime())) return "UPCOMING";
        if (showtime.getEndTime() != null && !now.isAfter(showtime.getEndTime())) return "PLAYING";
        return "ENDED";
    }

    public String displayStatusLabel(Showtime showtime) {
        return displayStatusLabel(displayStatus(showtime));
    }

    public String displayStatusLabel(String status) {
        return switch (normalize(status)) {
            case "CANCELLED" -> "Đã hủy";
            case "PLAYING", "NOW_SHOWING" -> "Đang chiếu";
            case "ENDED", "PAST" -> "Đã qua";
            default -> "Sắp chiếu";
        };
    }

    public String manualStatusLabel(String status) {
        return "CANCELLED".equals(normalize(status)) ? "Đã hủy" : "Sắp chiếu";
    }

    public String normalizeManualStatus(String status) {
        String normalized = normalize(status);
        if (normalized.isBlank()) return "SCHEDULED";
        if (SCHEDULED_ALIASES.contains(normalized)) return "SCHEDULED";
        if ("CANCELLED".equals(normalized)) return "CANCELLED";
        throw new IllegalArgumentException("Admin chỉ có thể đặt trạng thái Sắp chiếu hoặc Đã hủy.");
    }

    public boolean isEnded(Showtime showtime) {
        return "ENDED".equals(displayStatus(showtime));
    }

    public boolean isPastEndTime(Showtime showtime) {
        return showtime != null
                && showtime.getEndTime() != null
                && now().isAfter(showtime.getEndTime());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(timezone));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
