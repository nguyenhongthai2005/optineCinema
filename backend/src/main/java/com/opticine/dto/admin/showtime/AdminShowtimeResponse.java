package com.opticine.dto.admin.showtime;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminShowtimeResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long roomId;
    private String roomName;
    private String screenType;
    private Long timeSlotId;
    private String timeSlotName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String statusLabel;
    private String displayStatus;
    private String displayStatusLabel;
    private Boolean canEditStatus;
    private Boolean canBook;
    private Long totalSeats;
    private Long availableSeats;
    private Long bookedSeats;
}
