package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.GenreResponse;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.service.GenreService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.ReviewService;
import com.cinema.ticket_booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;
    private final ReviewService reviewService;
    private final BookingService bookingService;

    // GET /api/v1/movies?status=NOW_SHOWING&page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MovieResponse.Summary>>> getMovies(
            @RequestParam(defaultValue = "NOW_SHOWING") MovieStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(movieService.getByStatus(status, pageable)));
    }

    // GET /api/v1/movies/search?q=avengers
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<MovieResponse.Summary>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(movieService.search(q, pageable)));
    }

    // GET /api/v1/movies/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getById(id)));
    }

    // GET /api/v1/movies/cinema/{cinemaId} — phim đang chiếu tại rạp
    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<MovieResponse.Summary>>> getNowShowingByCinema(
            @PathVariable UUID cinemaId) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getNowShowingByCinema(cinemaId)));
    }

    // GET /api/v1/movies/{id}/reviews
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMovieReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(reviewService.getByMovie(id, pageable)));
    }

    // GET /api/v1/movies/{id}/can-review
    @GetMapping("/{id}/can-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> canReview(
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinema.ticket_booking.model.User currentUser) {
        UUID bookingId = bookingService.getEligibleBookingForReview(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(bookingId != null ? bookingId.toString() : null));
    }

    // POST /api/v1/movies [ADMIN]
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovieResponse>> create(
            @Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(movieService.create(request), "Tạo phim thành công"));
    }

    // PUT /api/v1/movies/{id} [ADMIN]
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovieResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MovieRequest request) {
        return ResponseEntity.ok(ApiResponse.success(movieService.update(id, request), "Cập nhật thành công"));
    }

    // DELETE /api/v1/movies/{id} [ADMIN]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        movieService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá phim"));
    }

    // ── Genre (nested) ────────────────────────────────────────────────────

    // GET /api/v1/movies/genres
    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getAllGenres() {
        return ResponseEntity.ok(ApiResponse.success(genreService.getAll()));
    }
}
