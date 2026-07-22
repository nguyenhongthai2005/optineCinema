package com.opticine.dto.admin.showtime;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminShowtimeRequest {

    @NotNull
    private Long movieId;

    @NotNull
    private Long roomId;

    private Long timeSlotId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @Size(max = 50)
    private String status;
}
