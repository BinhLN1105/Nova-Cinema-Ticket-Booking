package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.ReviewRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.Map;

public interface ReviewService {

    PageResponse<ReviewResponse> getByMovie(UUID movieId, Pageable pageable);

    PageResponse<ReviewResponse> getByMovieFiltered(UUID movieId, Integer ratingFilter, String sort, Pageable pageable);

    Map<Integer, Long> getStarDistribution(UUID movieId);

    ReviewResponse getExistingReview(UUID userId, UUID movieId);

    ReviewResponse create(UUID userId, ReviewRequest request);

    void delete(UUID userId, UUID reviewId);
}
