package com.opticine.service;

import com.opticine.dto.showtime.request.LockSeatRequest;
import com.opticine.dto.showtime.response.LockSeatResponse;
import com.opticine.dto.showtime.response.SeatEventDto;
import com.opticine.entity.Showtime;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.repository.ShowtimeRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeSeatService {

    private static final int LOCK_MINUTES = 10;

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository     showtimeRepository;
    private final SeatEventPublisher     seatEventPublisher;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;

    /** Hold ghế (lock). Release các ghế cũ của user này trong cùng suất nếu có. */
    @Transactional
    public LockSeatResponse lockSeats(Long showtimeId, LockSeatRequest request, Long userId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu id=" + showtimeId));
        showtimeAvailabilityService.requireBookableForSeatLock(showtime);

        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(LOCK_MINUTES);
        long expiredAtMillis    = expiredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Release ghế cũ của user (trường hợp user chọn lại)
        List<ShowtimeSeat> oldLocks =
                showtimeSeatRepository.findByShowtimeIdAndLockedBy(showtimeId, userId);
        List<Long> releasedSeatIds = oldLocks.stream()
                .map(ss -> ss.getSeat().getId())
                .collect(Collectors.toList());
        oldLocks.forEach(ss -> {
            ss.setStatus("AVAILABLE");
            ss.setLockedAt(null);
            ss.setLockedBy(null);
        });
        showtimeSeatRepository.saveAll(oldLocks);

        // Tìm các showtime_seat tương ứng với seatIds yêu cầu
        List<ShowtimeSeat> toLock = request.getSeatIds().stream()
                .map(seatId -> showtimeSeatRepository
                        .findByShowtimeIdAndSeatId(showtimeId, seatId)
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy ghế seatId=" + seatId + " trong suất " + showtimeId)))
                .collect(Collectors.toList());

        boolean hasMaintenance = toLock.stream()
                .anyMatch(ss -> ss.getSeat() != null && "MAINTENANCE".equalsIgnoreCase(ss.getSeat().getStatus()));
        if (hasMaintenance) {
            throw new RuntimeException("Ghế đang bảo trì, vui lòng chọn ghế khác.");
        }

        // Kiểm tra ghế AVAILABLE
        List<String> unavailable = toLock.stream()
                .filter(ss -> !"AVAILABLE".equals(ss.getStatus()))
                .map(ss -> ss.getSeat().getRowLabel() + ss.getSeat().getColumnNumber())
                .collect(Collectors.toList());
        if (!unavailable.isEmpty()) {
            throw new RuntimeException("Ghế đã được giữ hoặc đã bán: " + unavailable);
        }

        // Hold
        toLock.forEach(ss -> {
            ss.setStatus("LOCKED");
            ss.setLockedAt(now);
            ss.setLockedBy(userId);
        });
        List<ShowtimeSeat> saved = showtimeSeatRepository.saveAll(toLock);

        List<Long> heldSeatIds = saved.stream()
                .map(ss -> ss.getSeat().getId())
                .collect(Collectors.toList());
        List<Long> heldShowtimeSeatIds = saved.stream()
                .map(ShowtimeSeat::getId)
                .collect(Collectors.toList());

        // Tính tổng tiền có multiplier
        BigDecimal roomMul = showtime.getRoom().getPriceMultiplier() != null
                ? showtime.getRoom().getPriceMultiplier() : BigDecimal.ONE;
        BigDecimal slotMul = (showtime.getTimeSlot() != null && showtime.getTimeSlot().getPriceMultiplier() != null)
                ? showtime.getTimeSlot().getPriceMultiplier() : BigDecimal.ONE;

        BigDecimal total = saved.stream()
                .map(ss -> ss.getSeat().getBasePrice().multiply(roomMul).multiply(slotMul))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Broadcast — released event cho ghế cũ (nếu có), held event cho ghế mới
        if (!releasedSeatIds.isEmpty()) {
            seatEventPublisher.publish(showtimeId, SeatEventDto.builder()
                    .type(SeatEventDto.Type.SEAT_RELEASED)
                    .showtimeId(showtimeId)
                    .seatIds(releasedSeatIds)
                    .byUserId(userId)
                    .build());
        }
        seatEventPublisher.publish(showtimeId, SeatEventDto.builder()
                .type(SeatEventDto.Type.SEAT_HELD)
                .showtimeId(showtimeId)
                .seatIds(heldSeatIds)
                .byUserId(userId)
                .expiresAt(expiredAtMillis)
                .build());

        return LockSeatResponse.builder()
                .lockedShowtimeSeatIds(heldShowtimeSeatIds)
                .expiredAt(expiredAt)
                .totalPrice(total)
                .showtimeId(showtimeId)
                .build();
    }

    /** User chủ động huỷ chọn ghế. */
    @Transactional
    public void releaseSeats(Long showtimeId, Long userId) {
        List<ShowtimeSeat> locks =
                showtimeSeatRepository.findByShowtimeIdAndLockedBy(showtimeId, userId);
        if (locks.isEmpty()) return;

        List<Long> seatIds = locks.stream()
                .map(ss -> ss.getSeat().getId())
                .collect(Collectors.toList());
        locks.forEach(ss -> {
            ss.setStatus("AVAILABLE");
            ss.setLockedAt(null);
            ss.setLockedBy(null);
        });
        showtimeSeatRepository.saveAll(locks);

        seatEventPublisher.publish(showtimeId, SeatEventDto.builder()
                .type(SeatEventDto.Type.SEAT_RELEASED)
                .showtimeId(showtimeId)
                .seatIds(seatIds)
                .byUserId(userId)
                .build());
    }
}
