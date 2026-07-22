package com.opticine.dto.movie;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private String posterUrl;
    private String trailerUrl;
    private String trailerEmbedUrl;
    private String youtubeVideoId;
    private String genre;
    private Integer durationMinutes;
    private Integer duration;
    private String ageRating;
    private String status;
    private Integer popularityScore;
    private LocalDate releaseDate;
}
