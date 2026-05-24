package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.ReviewRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

        private final ReviewService reviewService;

        // GET /api/v1/reviews?movieId=...&page=0&size=10
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getByMovie(
                        @RequestParam UUID movieId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Integer rating,
                        @RequestParam(defaultValue = "highest") String sort) {
                var pageable = PageRequest.of(page, size);
                return ResponseEntity.ok(ApiResponse.success(
                                reviewService.getByMovieFiltered(movieId, rating, sort, pageable)));
        }

        // GET /api/v1/reviews/stats?movieId=...
        @GetMapping("/stats")
        public ResponseEntity<ApiResponse<java.util.Map<Integer, Long>>> getStarDistribution(
                        @RequestParam UUID movieId) {
                return ResponseEntity.ok(ApiResponse.success(
                                reviewService.getStarDistribution(movieId)));
        }

        // POST /api/v1/reviews
        @PostMapping
        public ResponseEntity<ApiResponse<ReviewResponse>> create(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody ReviewRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                reviewService.create(currentUser.getId(), request),
                                                "Đánh giá thành công"));
        }

        // PUT /api/v1/reviews/{id}
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<ReviewResponse>> update(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID id,
                        @Valid @RequestBody ReviewRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                                 reviewService.update(currentUser.getId(), id, request),
                                                 "Cập nhật đánh giá thành công"));
        }

        // DELETE /api/v1/reviews/{id} — ẩn review (soft delete)
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> delete(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID id) {
                reviewService.delete(currentUser.getId(), id);
                return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá đánh giá"));
        }
}
