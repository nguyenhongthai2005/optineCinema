package com.opticine.controller;

import com.opticine.dto.movie.MovieResponse;
import com.opticine.entity.Movie;
import com.opticine.repository.MovieRepository;
import com.opticine.util.YouTubeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies() {
        return ResponseEntity.ok(movieRepository.findByStatusOrderByPriorityAscTitleAsc("NOW_SHOWING")
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/now-showing")
    public ResponseEntity<List<MovieResponse>> getNowShowing() {
        return getMovies();
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<List<MovieResponse>> getComingSoon() {
        return ResponseEntity.ok(movieRepository.findByStatusOrderByPriorityAscTitleAsc("COMING_SOON")
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/hot")
    public ResponseEntity<List<MovieResponse>> getHotMovies() {
        return ResponseEntity.ok(movieRepository.findByStatusOrderByPopularityScoreDescTitleAsc("NOW_SHOWING", PageRequest.of(0, 3))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private MovieResponse toResponse(Movie movie) {
        String videoId = YouTubeUtils.extractVideoId(movie.getTrailerUrl()).orElse(null);
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .trailerEmbedUrl(YouTubeUtils.embedUrl(videoId))
                .youtubeVideoId(videoId)
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .duration(movie.getDurationMinutes())
                .ageRating(movie.getAgeRating())
                .status(movie.getStatus())
                .popularityScore(movie.getPopularityScore())
                .releaseDate(movie.getReleaseDate())
                .build();
    }
}
