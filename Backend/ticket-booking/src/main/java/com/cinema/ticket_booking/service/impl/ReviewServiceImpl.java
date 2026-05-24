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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

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
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByMovieFiltered(UUID movieId, Integer ratingFilter, String sort,
            Pageable pageable) {
        Sort sorting;
        if ("lowest".equalsIgnoreCase(sort)) {
            sorting = Sort.by("rating").ascending();
        } else if ("newest".equalsIgnoreCase(sort)) {
            sorting = Sort.by("createdAt").descending();
        } else {
            // default is highest
            sorting = Sort.by("rating").descending();
        }

        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);

        return PageResponse.of(
                reviewRepository.findByMovieFiltered(movieId, ratingFilter, newPageable)
                        .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStarDistribution(UUID movieId) {
        List<Object[]> results = reviewRepository.getStarDistribution(movieId);
        Map<Integer, Long> map = new HashMap<>();
        // Initialize with zeros for all 1-5 stars
        for (int i = 1; i <= 5; i++) {
            map.put(i, 0L);
        }
        for (Object[] row : results) {
            if (row != null && row.length == 2 && row[0] != null && row[1] != null) {
                Integer stars = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();
                map.put(stars, count);
            }
        }
        return map;
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

        if (existingReviewOpt.isPresent()) {
            throw new BadRequestException("Bạn đã đánh giá bộ phim này rồi, không thể đánh giá thêm.");
        }

        // Tạo review mới - Phải có vé đã xem
        if (bookingService.getEligibleBookingForReview(userId, movie.getId()) == null) {
            throw new BadRequestException("Bạn cần mua vé và xem phim trước khi đánh giá!");
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
    public ReviewResponse update(UUID userId, UUID reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review không tồn tại"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa review này");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setIsVisible(true);

        reviewRepository.save(review);

        // Cập nhật điểm trung bình phim
        Double newAvg = reviewRepository.calculateAvgRating(review.getMovie().getId());
        movieService.updateAvgRating(review.getMovie().getId(), newAvg);

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
