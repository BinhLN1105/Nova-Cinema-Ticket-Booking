package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ReviewRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.model.Review;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
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

        // Xác minh user đã thật sự xem phim
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Booking này không thuộc về bạn");
        }
        if (!booking.getShowtime().getMovie().getId().equals(movie.getId())) {
            throw new BadRequestException("Booking này không phải cho phim này");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Bạn cần thanh toán và xem phim trước khi đánh giá");
        }

        // Mỗi booking chỉ review 1 lần
        if (reviewRepository.existsByUserIdAndBookingId(userId, booking.getId())) {
            throw new ConflictException("Bạn đã đánh giá phim này rồi");
        }

        Review review = Review.builder()
                .user(user)
                .movie(movie)
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVisible(true)
                .build();
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
