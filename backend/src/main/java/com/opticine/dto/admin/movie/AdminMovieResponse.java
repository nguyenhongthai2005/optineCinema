package com.opticine.dto.admin.movie;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminMovieResponse {
    private Long id;
    private String title;
    private String description;
    private String genre;
    private Integer durationMinutes;
    private String ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String posterUrl;
    private String trailerUrl;
    private String trailerEmbedUrl;
    private String youtubeVideoId;
    private String status;
    private Integer popularityScore;
    private Integer priority;
    private Long showtimeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
