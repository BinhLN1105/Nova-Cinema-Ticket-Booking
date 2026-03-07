package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Lấy review của 1 phim (chỉ hiển thị review công khai, mới nhất trước)
    Page<Review> findByMovieIdAndIsVisibleTrueOrderByCreatedAtDesc(UUID movieId, Pageable pageable);

    // Kiểm tra user đã review booking này chưa (tránh duplicate)
    boolean existsByUserIdAndBookingId(UUID userId, UUID bookingId);

    // Tính lại điểm trung bình sau khi thêm review mới
    @Query("""
                SELECT AVG(r.rating) FROM Review r
                WHERE r.movie.id = :movieId
                  AND r.isVisible = true
            """)
    Double calculateAvgRating(@Param("movieId") UUID movieId);

    // Lấy review của user
    Page<Review> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
