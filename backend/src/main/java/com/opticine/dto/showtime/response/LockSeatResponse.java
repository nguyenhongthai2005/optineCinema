package com.opticine.dto.showtime.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trả về sau khi lock ghế thành công.
 * M3 (Booking) dùng `lockedShowtimeSeatIds` để tạo booking.
 */
@Data
@Builder
public class LockSeatResponse {

    private List<Long>    lockedShowtimeSeatIds;
    private LocalDateTime expiredAt;
    private BigDecimal    totalPrice;
    private Long          showtimeId;
}
