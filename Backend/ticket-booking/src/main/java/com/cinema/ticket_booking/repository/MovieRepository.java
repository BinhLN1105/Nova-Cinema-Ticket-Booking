package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    // Lấy phim theo trạng thái (NOW_SHOWING, COMING_SOON, ENDED)
    Page<Movie> findByStatus(MovieStatus status, Pageable pageable);

    // Tìm kiếm phim theo tên (không phân biệt hoa thường)
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Lấy phim theo thể loại
    @Query("""
        SELECT DISTINCT m FROM Movie m
        JOIN m.genres g
        WHERE g.id = :genreId AND m.status = :status
    """)
    Page<Movie> findByGenreIdAndStatus(
        @Param("genreId") Integer genreId,
        @Param("status") MovieStatus status,
        Pageable pageable
    );

    // Lấy phim đang chiếu có suất chiếu tại một rạp cụ thể
    @Query("""
        SELECT DISTINCT m FROM Movie m
        JOIN Showtime s ON s.movie = m
        JOIN s.screen sc
        WHERE sc.cinema.id = :cinemaId
          AND m.status = 'NOW_SHOWING'
          AND s.status = 'SCHEDULED'
        ORDER BY m.title
    """)
    List<Movie> findNowShowingByCinema(@Param("cinemaId") UUID cinemaId);

    // Top phim được đánh giá cao nhất
    List<Movie> findTop10ByStatusOrderByAvgRatingDesc(MovieStatus status);

    long countByStatus(MovieStatus status);
}
