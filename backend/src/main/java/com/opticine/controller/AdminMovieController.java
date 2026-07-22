package com.opticine.controller;

import com.opticine.dto.admin.movie.AdminMovieRequest;
import com.opticine.dto.admin.movie.AdminMovieResponse;
import com.opticine.dto.admin.movie.AdminMovieStatusRequest;
import com.opticine.service.AdminMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/movies")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    @GetMapping
    public ResponseEntity<List<AdminMovieResponse>> getMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String genre
    ) {
        return ResponseEntity.ok(adminMovieService.findMovies(keyword, status, genre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMovieResponse> getMovie(@PathVariable Long id) {
        return ResponseEntity.ok(adminMovieService.getMovie(id));
    }

    @PostMapping
    public ResponseEntity<AdminMovieResponse> createMovie(@RequestBody AdminMovieRequest request) {
        return ResponseEntity.ok(adminMovieService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminMovieResponse> updateMovie(@PathVariable Long id, @RequestBody AdminMovieRequest request) {
        return ResponseEntity.ok(adminMovieService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminMovieResponse> updateStatus(@PathVariable Long id, @RequestBody AdminMovieStatusRequest request) {
        return ResponseEntity.ok(adminMovieService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        adminMovieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
