package com.opticine.service;

import com.opticine.dto.showtime.response.SeatStatusResponse;
import com.opticine.dto.showtime.response.ShowtimeResponse;
import com.opticine.entity.Showtime;
import com.opticine.repository.ShowtimeRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository     showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;
    private final ShowtimeStatusService showtimeStatusService;

    /** Lấy suất chiếu theo ngày (và phim nếu có movieId). */
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimes(Long movieId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();

        List<Showtime> list = (movieId != null)
                ? showtimeRepository.findByMovieIdAndStartTimeBetween(movieId, from, to)
                : showtimeRepository.findByStartTimeBetween(from, to);

        return list.stream()
                .filter(showtimeAvailabilityService::isBookable)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu id=" + showtimeId));
        showtimeAvailabilityService.requireBookableForSeatLock(showtime);
        return toResponse(showtime);
    }

    /** Lấy sơ đồ ghế + trạng thái của 1 suất chiếu. */
    @Transactional(readOnly = true)
    public List<SeatStatusResponse> getSeatsForShowtime(Long showtimeId) {
        return getSeatsForShowtime(showtimeId, null);
    }

    @Transactional(readOnly = true)
    public List<SeatStatusResponse> getSeatsForShowtime(Long showtimeId, Long currentUserId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu id=" + showtimeId));
        showtimeAvailabilityService.requireBookableForSeatLock(showtime);

        BigDecimal roomMultiplier = showtime.getRoom().getPriceMultiplier() != null
                ? showtime.getRoom().getPriceMultiplier()
                : BigDecimal.ONE;

        BigDecimal slotMultiplier = (showtime.getTimeSlot() != null
                && showtime.getTimeSlot().getPriceMultiplier() != null)
                ? showtime.getTimeSlot().getPriceMultiplier()
                : BigDecimal.ONE;

        return showtimeSeatRepository.findByShowtimeId(showtimeId)
                .stream()
                .map(ss -> {
                    BigDecimal finalPrice = ss.getSeat().getBasePrice()
                            .multiply(roomMultiplier)
                            .multiply(slotMultiplier);
                    boolean lockedByCurrentUser = "LOCKED".equals(ss.getStatus())
                            && currentUserId != null
                            && ss.getLockedBy() != null
                            && ss.getLockedBy().equals(currentUserId);
                    boolean maintenance = ss.getSeat() != null && "MAINTENANCE".equalsIgnoreCase(ss.getSeat().getStatus());
                    String effectiveStatus = maintenance ? "MAINTENANCE" : ss.getStatus();
                    return SeatStatusResponse.builder()
                            .showtimeSeatId(ss.getId())
                            .seatId(ss.getSeat().getId())
                            .rowLabel(ss.getSeat().getRowLabel())
                            .columnNumber(ss.getSeat().getColumnNumber())
                            .seatType(ss.getSeat().getSeatType())
                            .seatTypeLabel(seatTypeLabel(ss.getSeat().getSeatType()))
                            .basePrice(ss.getSeat().getBasePrice())
                            .roomMultiplier(roomMultiplier)
                            .timeSlotMultiplier(slotMultiplier)
                            .roomName(showtime.getRoom().getName())
                            .screenType(showtime.getRoom().getScreenType())
                            .timeSlotName(showtime.getTimeSlot() != null ? showtime.getTimeSlot().getName() : null)
                            .finalPrice(finalPrice)
                            .status(effectiveStatus)
                            .statusLabel(statusLabel(effectiveStatus))
                            .maintenance(maintenance)
                            .lockedByCurrentUser(lockedByCurrentUser)
                            .lockExpiresAt(ss.getLockedAt() != null ? ss.getLockedAt().plusMinutes(10) : null)
                            .pairedSeatId(ss.getSeat().getPairedSeat() != null
                                    ? ss.getSeat().getPairedSeat().getId() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String seatTypeLabel(String seatType) {
        if ("VIP".equalsIgnoreCase(seatType)) return "VIP";
        if ("COUPLE".equalsIgnoreCase(seatType)) return "Đôi";
        if ("PREMIUM".equalsIgnoreCase(seatType)) return "Premium";
        return "Thường";
    }

    private String statusLabel(String status) {
        if ("MAINTENANCE".equalsIgnoreCase(status)) return "Bảo trì";
        if ("BOOKED".equalsIgnoreCase(status)) return "Đã bán";
        if ("LOCKED".equalsIgnoreCase(status)) return "Đang được giữ";
        return "Có thể chọn";
    }

    private ShowtimeResponse toResponse(Showtime s) {
        long available = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "AVAILABLE");
        String displayStatus = showtimeStatusService.displayStatus(s);
        return ShowtimeResponse.builder()
                .id(s.getId())
                .movieId(s.getMovie().getId())
                .movieTitle(s.getMovie().getTitle())
                .moviePosterUrl(s.getMovie().getPosterUrl())
                .movieDurationMinutes(s.getMovie().getDurationMinutes())
                .movieAgeRating(s.getMovie().getAgeRating())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .screenType(s.getRoom().getScreenType())
                .timeSlotName(s.getTimeSlot() != null ? s.getTimeSlot().getName() : null)
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .statusLabel(showtimeStatusService.manualStatusLabel(s.getStatus()))
                .displayStatus(displayStatus)
                .displayStatusLabel(showtimeStatusService.displayStatusLabel(displayStatus))
                .canBook(showtimeAvailabilityService.isBookable(s))
                .availableSeats(available)
                .build();
    }
}
