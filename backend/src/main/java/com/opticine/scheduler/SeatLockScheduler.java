package com.opticine.scheduler;

import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import com.opticine.service.SeatEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.opticine.entity.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private static final int LOCK_MINUTES = 2;

    private final ShowtimeSeatRepository seatRepository;
    private final BookingRepository      bookingRepository;
    private final SeatEventPublisher     seatEventPublisher;

    /**
     * Chạy mỗi 60 giây.
     * Release ghế LOCKED quá 10 phút về AVAILABLE.
     * Broadcast SEAT_RELEASED để client cập nhật ngay.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(LOCK_MINUTES);
        List<ShowtimeSeat> expired =
                seatRepository.findByStatusAndLockedAtBefore("LOCKED", cutoff).stream()
                        .filter(seat -> bookingRepository.findByShowtimeSeatId(seat.getId()).stream()
                                .noneMatch(booking -> "WAITING_CONFIRMATION".equals(booking.getPaymentStatus())))
                        .toList();

        if (expired.isEmpty()) return;

        // Group seatIds theo showtimeId để broadcast 1 message / showtime
        Map<Long, List<Long>> byShowtime = expired.stream()
                .collect(Collectors.groupingBy(
                        ss -> ss.getShowtime().getId(),
                        Collectors.mapping(ss -> ss.getSeat().getId(), Collectors.toList())
                ));

        expired.forEach(ss -> {
            ss.setStatus("AVAILABLE");
            ss.setLockedAt(null);
            ss.setLockedBy(null);
        });
        seatRepository.saveAll(expired);

        byShowtime.forEach((showtimeId, seatIds) -> {
            seatEventPublisher.publish(showtimeId, SeatEventDto.builder()
                    .type(SeatEventDto.Type.SEAT_RELEASED)
                    .showtimeId(showtimeId)
                    .seatIds(seatIds)
                    .byUserId(null) // null = system/scheduler
                    .build());
        });

        // Hủy các Booking chưa thanh toán
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiredAtBefore("PENDING_PAYMENT", LocalDateTime.now());
        if (!expiredBookings.isEmpty()) {
            expiredBookings.forEach(b -> {
                b.setStatus("CANCELLED");
                b.setPaymentStatus("CANCELLED");
            });
            bookingRepository.saveAll(expiredBookings);
            log.info("[SeatLockScheduler] Auto-cancelled {} expired booking(s)", expiredBookings.size());
        }

        log.info("[SeatLockScheduler] Released {} expired seat(s) across {} showtime(s)",
                expired.size(), byShowtime.size());
    }
}
