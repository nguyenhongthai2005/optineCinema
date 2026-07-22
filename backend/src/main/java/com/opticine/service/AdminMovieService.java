package com.opticine.service;

import com.opticine.dto.admin.movie.AdminMovieRequest;
import com.opticine.dto.admin.movie.AdminMovieResponse;
import com.opticine.entity.Movie;
import com.opticine.repository.MovieRepository;
import com.opticine.repository.ShowtimeRepository;
import com.opticine.util.YouTubeUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminMovieService {
    private static final List<String> STATUSES = List.of("NOW_SHOWING", "COMING_SOON", "STOPPED");

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    @Transactional(readOnly = true)
    public List<AdminMovieResponse> findMovies(String keyword, String status, String genre) {
        Specification<Movie> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("genre")), like)
                ));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), normalizeStatus(status)));
            }
            if (genre != null && !genre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("genre")), "%" + genre.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return movieRepository.findAll(spec, Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("title")))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminMovieResponse getMovie(Long id) {
        return toResponse(findMovie(id));
    }

    @Transactional
    public AdminMovieResponse create(AdminMovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public AdminMovieResponse update(Long id, AdminMovieRequest request) {
        Movie movie = findMovie(id);
        apply(movie, request);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public AdminMovieResponse updateStatus(Long id, String status) {
        Movie movie = findMovie(id);
        String normalized = normalizeStatus(status);
        if ("STOPPED".equals(normalized) && hasFutureActiveShowtimes(id)) {
            throw new IllegalArgumentException("Không thể ngừng chiếu phim đang có lịch chiếu sắp tới.");
        }
        movie.setStatus(normalized);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public void delete(Long id) {
        Movie movie = findMovie(id);
        if (showtimeRepository.existsByMovieId(id)) {
            if (hasFutureActiveShowtimes(id)) {
                throw new IllegalArgumentException("Không thể ngừng chiếu phim đang có lịch chiếu sắp tới.");
            }
            movie.setStatus("STOPPED");
            movieRepository.save(movie);
            return;
        }
        movieRepository.delete(movie);
    }

    public AdminMovieResponse toResponse(Movie movie) {
        String videoId = YouTubeUtils.extractVideoId(movie.getTrailerUrl()).orElse(null);
        return AdminMovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .ageRating(movie.getAgeRating())
                .releaseDate(movie.getReleaseDate())
                .endDate(movie.getEndDate())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .trailerEmbedUrl(YouTubeUtils.embedUrl(videoId))
                .youtubeVideoId(videoId)
                .status(movie.getStatus())
                .popularityScore(movie.getPopularityScore())
                .priority(movie.getPriority())
                .showtimeCount(showtimeRepository.countByMovieId(movie.getId()))
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    private Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim."));
    }

    private void apply(Movie movie, AdminMovieRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên phim là bắt buộc.");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Thời lượng phim phải lớn hơn 0.");
        }
        if (request.getPopularityScore() != null && (request.getPopularityScore() < 1 || request.getPopularityScore() > 100)) {
            throw new IllegalArgumentException("Độ hot phải nằm trong khoảng 1 đến 100.");
        }
        String trailerUrl = blankToNull(request.getTrailerUrl());
        if (trailerUrl != null && YouTubeUtils.extractVideoId(trailerUrl).isEmpty()) {
            throw new IllegalArgumentException("Link trailer YouTube không hợp lệ.");
        }

        movie.setTitle(request.getTitle().trim());
        movie.setDescription(blankToNull(request.getDescription()));
        movie.setGenre(blankToNull(request.getGenre()));
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(blankToNull(request.getAgeRating()));
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setPosterUrl(blankToNull(request.getPosterUrl()));
        movie.setTrailerUrl(trailerUrl);
        movie.setStatus(normalizeStatus(request.getStatus()));
        movie.setPopularityScore(request.getPopularityScore() == null ? 50 : request.getPopularityScore());
        movie.setPriority(request.getPriority());
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "NOW_SHOWING" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái phim không hợp lệ.");
        }
        return normalized;
    }

    private boolean hasFutureActiveShowtimes(Long movieId) {
        return showtimeRepository.existsByMovieIdAndStartTimeAfterAndStatusNot(movieId, LocalDateTime.now(), "CANCELLED");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
