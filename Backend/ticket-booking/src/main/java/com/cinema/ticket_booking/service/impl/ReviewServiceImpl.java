package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ReviewRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.model.Review;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.ReviewMapper;
import com.cinema.ticket_booking.repository.ReviewRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.ReviewService;
import com.cinema.ticket_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final MovieService movieService;
    private final BookingService bookingService;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getExistingReview(UUID userId, UUID movieId) {
        return reviewRepository.findByUserIdAndMovieId(userId, movieId)
                .map(reviewMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByMovie(UUID movieId, Pageable pageable) {
        return PageResponse.of(
                reviewRepository.findByMovieIdAndIsVisibleTrueOrderByCreatedAtDesc(movieId, pageable)
                        .map(reviewMapper::toResponse));
    }

    @Override
    public ReviewResponse create(UUID userId, ReviewRequest request) {
        User user = userService.findById(userId);
        Movie movie = movieService.findById(UUID.fromString(request.getMovieId()));
        Booking booking = bookingService.findById(UUID.fromString(request.getBookingId()));

        // 1. Kiểm tra tính hợp lệ cơ bản
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Booking này không thuộc về bạn");
        }
        if (!booking.getShowtime().getMovie().getId().equals(movie.getId())) {
            throw new BadRequestException("Booking này không phải cho phim này");
        }

        // 2. Kiểm tra xem đã có review cho phim này chưa (1 Review / 1 Phim / 1
        // Account)
        var existingReviewOpt = reviewRepository.findByUserIdAndMovieId(userId, movie.getId());

        Review review;
        if (existingReviewOpt.isPresent()) {
            // Cập nhật review cũ
            review = existingReviewOpt.get();
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setBooking(booking); // Cập nhật sang booking mới nhất (nếu muốn)
        } else {
            // Tạo review mới - Phải có vé đã xem
            if (bookingService.getEligibleBookingForReview(userId, movie.getId()) == null) {
                throw new BadRequestException("Bạn cần mua vé và xem phim trước khi đánh giá!");
            }

            review = Review.builder()
                    .user(user)
                    .movie(movie)
                    .booking(booking)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .isVisible(true)
                    .build();
        }

        reviewRepository.save(review);

        // Cập nhật điểm trung bình phim
        Double newAvg = reviewRepository.calculateAvgRating(movie.getId());
        movieService.updateAvgRating(movie.getId(), newAvg);

        return reviewMapper.toResponse(review);
    }

    @Override
    public void delete(UUID userId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xoá review này");
        }

        review.setIsVisible(false);
        reviewRepository.save(review);
    }
}
