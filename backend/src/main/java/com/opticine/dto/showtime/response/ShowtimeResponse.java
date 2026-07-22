package com.opticine.dto.showtime.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShowtimeResponse {

    private Long id;

    // Thông tin phim
    private Long    movieId;
    private String  movieTitle;
    private String  moviePosterUrl;
    private Integer movieDurationMinutes;
    private String  movieAgeRating;

    // Thông tin phòng
    private Long   roomId;
    private String roomName;
    private String screenType;

    // Khung giờ
    private String timeSlotName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String        status;
    private String        statusLabel;
    private String        displayStatus;
    private String        displayStatusLabel;
    private boolean       canBook;

    private long availableSeats;
}
