package com.opticine.dto.showtime.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Event broadcast qua WebSocket topic /topic/showtimes/{id}/seats
 *
 * Tham khảo: AstraCine SeatEventDto.
 * Khác biệt: Opticine dùng DB lock thay vì Redis nên không có holdId UUID,
 * byUserId chính là user ID của người thao tác.
 */
@Data
@Builder
public class SeatEventDto {

    public enum Type {
        SEAT_HELD,      // user giữ ghế (lock)
        SEAT_RELEASED,  // huỷ giữ (do user hoặc do hết hạn)
        SEAT_SOLD       // đã thanh toán xong, M3 sẽ broadcast cái này
    }

    private Type type;
    private Long showtimeId;
    private List<Long> seatIds;     // seatId chứ không phải showtimeSeatId
    private Long byUserId;          // ai làm action này (null nếu là scheduler)
    private Long expiresAt;         // epoch millis, chỉ có khi SEAT_HELD
}
