package com.opticine.dto.admin.movie;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminMovieRequest {
    private String title;
    private String description;
    private String genre;
    private Integer durationMinutes;
    private String ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String posterUrl;
    private String trailerUrl;
    private String status;
    private Integer popularityScore;
    private Integer priority;
}
