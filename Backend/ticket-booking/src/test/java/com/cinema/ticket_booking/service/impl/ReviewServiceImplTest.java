package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.mapper.ReviewMapper;
import com.cinema.ticket_booking.model.Review;
import com.cinema.ticket_booking.repository.ReviewRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserService userService;
    @Mock
    private MovieService movieService;
    @Mock
    private BookingService bookingService;
    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID userId;
    private UUID movieId;
    private Review review;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        movieId = UUID.randomUUID();

        review = Review.builder()
                .id(UUID.randomUUID())
                .rating(5)
                .comment("Phim rất hay!")
                .isVisible(true)
                .build();

        reviewResponse = ReviewResponse.builder()
                .id(review.getId().toString())
                .rating(5)
                .comment("Phim rất hay!")
                .build();
    }

    @Test
    @DisplayName("1. getExistingReview trả về review nếu đã đánh giá")
    void testGetExistingReviewFound() {
        when(reviewRepository.findByUserIdAndMovieId(userId, movieId)).thenReturn(Optional.of(review));
        when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

        ReviewResponse result = reviewService.getExistingReview(userId, movieId);
        assertNotNull(result);
        assertEquals(5, result.getRating());
    }

    @Test
    @DisplayName("2. getExistingReview trả về null nếu chưa đánh giá")
    void testGetExistingReviewNotFound() {
        when(reviewRepository.findByUserIdAndMovieId(userId, movieId)).thenReturn(Optional.empty());

        ReviewResponse result = reviewService.getExistingReview(userId, movieId);
        assertNull(result);
    }

    @Test
    @DisplayName("3. getByMovie trả về danh sách review phân trang")
    void testGetByMovie() {
        Page<Review> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        when(reviewRepository.findByMovieIdAndIsVisibleTrueOrderByCreatedAtDesc(eq(movieId), any(Pageable.class)))
                .thenReturn(page);
        when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

        PageResponse<ReviewResponse> result = reviewService.getByMovie(movieId, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }
}
