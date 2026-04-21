package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Showtime;
// import com.cinema.ticket_booking.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
      @Param("date") LocalDate date);

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
      @Param("date") LocalDate date);

  // Lấy tất cả lịch chiếu tại 1 rạp theo ngày (không phân biệt phim)
  @Query("""
          SELECT s FROM Showtime s
          WHERE s.screen.cinema.id = :cinemaId
            AND CAST(s.startTime AS date) = :date
            AND s.status = 'SCHEDULED'
          ORDER BY s.startTime
      """)
  List<Showtime> findByCinemaAndDateScheduled(
      @Param("cinemaId") UUID cinemaId,
      @Param("date") LocalDate date);

  // Lấy suất chiếu đang diễn ra hoặc sắp bắt đầu (dùng cho Scheduler cập nhật
  // status)
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
      @Param("endTime") LocalDateTime endTime);

  // Kiểm tra xem phòng chiếu đã từng có lịch chiếu nào chưa (dùng cho Xóa cứng)
  boolean existsByScreenId(UUID screenId);

  @Query("SELECT s FROM Showtime s WHERE s.screen.cinema.id = :cinemaId AND CAST(s.startTime AS date) = :date")
  Page<Showtime> findByCinemaIdAndDate(@Param("cinemaId") UUID cinemaId, @Param("date") LocalDate date,
      Pageable pageable);

  @Query("SELECT s FROM Showtime s WHERE s.screen.cinema.id = :cinemaId")
  Page<Showtime> findByCinemaId(@Param("cinemaId") UUID cinemaId, Pageable pageable);

  @Query("SELECT s FROM Showtime s WHERE CAST(s.startTime AS date) = :date")
  Page<Showtime> findByDate(@Param("date") LocalDate date, Pageable pageable);

  @Query("SELECT s FROM Showtime s WHERE s.status = ShowtimeStatus.SCHEDULED AND s.startTime <= :nowMinusAllowance AND :now < s.endTime")
  List<Showtime> findShowtimesToMarkOngoing(@Param("nowMinusAllowance") LocalDateTime nowMinusAllowance,
      @Param("now") LocalDateTime now);

  @Query("SELECT s FROM Showtime s WHERE s.status IN (ShowtimeStatus.SCHEDULED, ShowtimeStatus.ONGOING) AND s.endTime <= :now")
  List<Showtime> findShowtimesToMarkFinished(@Param("now") LocalDateTime now);

  // Kiểm tra xem phim đã từng có suất chiếu nào chưa
  boolean existsByMovieId(UUID movieId);

  // Đếm số suất chiếu (không huỷ) của rạp trong ngày — dùng cho Staff Dashboard
  @Query("""
      SELECT COUNT(s) FROM Showtime s
      WHERE s.screen.cinema.id = :cinemaId
        AND s.status != 'CANCELLED'
        AND CAST(s.startTime AS date) = :date
  """)
  long countByCinemaAndDate(
      @Param("cinemaId") UUID cinemaId,
      @Param("date") java.time.LocalDate date);

  // Lấy suất chiếu sắp bắt đầu trong 60 phút tới — dùng cho Staff Dashboard
  @Query("""
      SELECT s FROM Showtime s
      JOIN FETCH s.movie m
      JOIN FETCH s.screen sc
      WHERE sc.cinema.id = :cinemaId
        AND s.status != 'CANCELLED'
        AND s.startTime >= :from
        AND s.startTime <= :until
      ORDER BY s.startTime ASC
  """)
  List<Showtime> findUpcomingByCinema(
      @Param("cinemaId") UUID cinemaId,
      @Param("from") LocalDateTime from,
      @Param("until") LocalDateTime until);
}
