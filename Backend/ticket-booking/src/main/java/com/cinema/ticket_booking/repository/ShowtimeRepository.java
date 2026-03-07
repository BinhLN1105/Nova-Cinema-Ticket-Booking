package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {

    // Lấy lịch chiếu của 1 phim theo ngày
    @Query("""
        SELECT s FROM Showtime s
        WHERE s.movie.id = :movieId
          AND CAST(s.startTime AS date) = :date
          AND s.status = 'SCHEDULED'
        ORDER BY s.startTime
    """)
    List<Showtime> findByMovieAndDate(
        @Param("movieId") UUID movieId,
        @Param("date") LocalDate date
    );

    // Lấy lịch chiếu của 1 phim tại 1 rạp theo ngày
    @Query("""
        SELECT s FROM Showtime s
        WHERE s.movie.id = :movieId
          AND s.screen.cinema.id = :cinemaId
          AND CAST(s.startTime AS date) = :date
          AND s.status = 'SCHEDULED'
        ORDER BY s.startTime
    """)
    List<Showtime> findByMovieAndCinemaAndDate(
        @Param("movieId") UUID movieId,
        @Param("cinemaId") UUID cinemaId,
        @Param("date") LocalDate date
    );

    // Lấy suất chiếu đang diễn ra hoặc sắp bắt đầu (dùng cho Scheduler cập nhật status)
    @Query("""
        SELECT s FROM Showtime s
        WHERE s.status = 'SCHEDULED'
          AND s.startTime <= :now
    """)
    List<Showtime> findShowtimesToMarkOngoing(@Param("now") LocalDateTime now);

    // Kiểm tra xung đột lịch phòng chiếu (tránh đặt 2 suất trùng giờ)
    @Query("""
        SELECT COUNT(s) > 0 FROM Showtime s
        WHERE s.screen.id = :screenId
          AND s.status != 'CANCELLED'
          AND s.startTime < :endTime
          AND s.endTime > :startTime
    """)
    boolean existsConflict(
        @Param("screenId") UUID screenId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
