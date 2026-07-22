package com.opticine.dto.showtime.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LockSeatRequest {

    @NotEmpty(message = "Phải chọn ít nhất 1 ghế")
    private List<Long> seatIds;   // Danh sách Seat ID (không phải ShowtimeSeat ID)
}
