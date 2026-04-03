package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

  // Tìm theo mã booking (dùng cho check-in QR)
  Optional<Booking> findByBookingCode(String bookingCode);

  // Tìm theo QR code string
  Optional<Booking> findByQrCode(String qrCode);

  // Lịch sử đặt vé của user (phân trang)
  @EntityGraph(attributePaths = { "showtime.movie", "showtime.screen.cinema" })
  Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  // Đơn đặt vé của user theo trạng thái
  List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

  // Admin: lọc booking theo trạng thái (phân trang)
  Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

  // Admin: xem booking theo suất chiếu
  List<Booking> findByShowtimeId(UUID showtimeId);

  // Lấy booking theo suất chiếu và trạng thái
  List<Booking> findByShowtimeIdAndStatus(UUID showtimeId, BookingStatus status);

  // Tìm booking theo trạng thái và thời gian hết hạn (dùng cho Scheduler)
  List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);

  // Kiểm tra xem rạp có booking nào không (Dùng để bảo vệ khi xóa rạp)
  @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN b.showtime s JOIN s.screen sc WHERE sc.cinema.id = :cinemaId")
  boolean existsByCinemaId(@Param("cinemaId") UUID cinemaId);

  // ── Atomic Actions ──────────────────────────────────────────────────────
  @Modifying
  @Query("UPDATE Booking b SET b.status = 'CANCELLED', b.cancellationToken = null, b.cancellationTokenExpiry = null, b.earnedExp = 0 WHERE b.id = :bookingId AND b.status = 'PAID'")
  int cancelPaidBooking(@Param("bookingId") UUID bookingId);

  // ── Scheduler: tự động EXPIRED booking PENDING quá hạn ───────────────
  @Modifying
  @Query("""
          UPDATE Booking b
          SET b.status = 'EXPIRED'
          WHERE b.status = 'PENDING'
            AND b.expiresAt < :now
      """)
  int expireOverdueBookings(@Param("now") LocalDateTime now);

  // ── Scheduler: tìm booking sắp đến giờ chiếu để gửi nhắc nhở ───────
  @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.showtime.startTime >= :fromTime AND b.showtime.startTime < :toTime")
  List<Booking> findUpcomingBookings(
      @Param("status") BookingStatus status,
      @Param("fromTime") LocalDateTime fromTime,
      @Param("toTime") LocalDateTime toTime);

  // Lấy booking hợp lệ để review phim (Verified Purchase Review)
  // Điều kiện: Booking PAID, VÀ (suất chiếu đã bắt đầu HOẶC có ít nhất 1 vé đã
  // check-in)
  @Query(value = """
          SELECT b.* FROM bookings b
          JOIN showtimes s ON b.showtime_id = s.id
          WHERE b.user_id = :userId
            AND s.movie_id = :movieId
            AND b.status = 'CHECKED_IN'
          ORDER BY b.created_at DESC LIMIT 1
      """, nativeQuery = true)
  Optional<Booking> findEligibleBookingForReview(
      @Param("userId") UUID userId,
      @Param("movieId") UUID movieId);

  // ── Exp Conversion ───────────────────────────────────────────────────

  @Query("""
          SELECT b FROM Booking b
          WHERE b.status = :status
          AND b.expAdded = false
          AND b.showtime.startTime < :thresholdDate
      """)
  List<Booking> findBookingsForExpConversion(
      @Param("status") BookingStatus status,
      @Param("thresholdDate") LocalDateTime thresholdDate);

  // ── Dashboard Admin Queries ──────────────────────────────────────────

  @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
      "JOIN b.showtime s JOIN s.screen sc " +
      "WHERE b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED') " +
      "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
      "AND (:cinemaId IS NULL OR sc.cinema.id = :cinemaId)")
  BigDecimal calculateNetRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

  @Query("SELECT COUNT(b) FROM Booking b " +
      "JOIN b.showtime s JOIN s.screen sc " +
      "WHERE b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED') " +
      "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
      "AND (:cinemaId IS NULL OR sc.cinema.id = :cinemaId)")
  long countTotalBookingsByDateRange(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

  @Query("SELECT COALESCE(SUM(COALESCE(b.discountAmount, 0) + COALESCE(b.promotionDiscountAmount, 0)), 0) FROM Booking b " +
      "JOIN b.showtime s JOIN s.screen sc " +
      "WHERE b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED') " +
      "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
      "AND (:cinemaId IS NULL OR sc.cinema.id = :cinemaId)")
  BigDecimal calculateTotalDiscounts(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

  @Query("SELECT CAST(s.seatType AS string) AS name, SUM(bi.seatPrice) AS grossRevenue " +
      "FROM BookingItem bi JOIN bi.booking b JOIN bi.showtimeSeat ss JOIN ss.seat s " +
      "JOIN b.showtime st JOIN st.screen sc " +
      "WHERE b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED') " +
      "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
      "AND (:cinemaId IS NULL OR sc.cinema.id = :cinemaId) " +
      "GROUP BY s.seatType")
  List<RevenueBreakdownProjection> getTicketRevenueBySeatType(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

  @Query("SELECT c.name AS name, SUM(bc.unitPrice * bc.quantity) AS grossRevenue " +
      "FROM BookingCombo bc JOIN bc.booking b JOIN bc.combo c " +
      "JOIN b.showtime st JOIN st.screen sc " +
      "WHERE b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED') " +
      "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
      "AND (:cinemaId IS NULL OR sc.cinema.id = :cinemaId) " +
      "GROUP BY c.name")
  List<RevenueBreakdownProjection> getConcessionRevenueByCombo(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

  @Query(value = """
          SELECT
              TO_CHAR(b.created_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') as date,
              SUM(b.total_amount) as revenue,
              COUNT(b.id) as bookingCount
          FROM bookings b
          JOIN showtimes s ON b.showtime_id = s.id
          JOIN screens sc ON s.screen_id = sc.id
          WHERE b.created_at >= :startDate AND b.created_at <= :endDate
            AND b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED')
            AND (:cinemaId IS NULL OR sc.cinema_id = :cinemaId)
          GROUP BY 1
          ORDER BY 1
      """, nativeQuery = true)
  List<RevenueByDayProjection> getRevenueByDayInRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("cinemaId") UUID cinemaId);

  @Query(value = """
          SELECT
              TO_CHAR(b.created_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') as date,
              SUM(bi.seat_price) as revenue
          FROM bookings b
          JOIN booking_items bi ON bi.booking_id = b.id
          JOIN showtimes s ON b.showtime_id = s.id
          JOIN screens sc ON s.screen_id = sc.id
          WHERE b.created_at >= :startDate AND b.created_at <= :endDate
            AND b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED')
            AND (:cinemaId IS NULL OR sc.cinema_id = :cinemaId)
          GROUP BY 1
          ORDER BY 1
      """, nativeQuery = true)
  List<RevenueByDayProjection> getDailyTicketRevenueInRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("cinemaId") UUID cinemaId);

  @Query(value = """
          SELECT
              TO_CHAR(b.created_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') as date,
              SUM(bc.unit_price * bc.quantity) as revenue
          FROM bookings b
          JOIN booking_combos bc ON bc.booking_id = b.id
          JOIN showtimes s ON b.showtime_id = s.id
          JOIN screens sc ON s.screen_id = sc.id
          WHERE b.created_at >= :startDate AND b.created_at <= :endDate
            AND b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED')
            AND (:cinemaId IS NULL OR sc.cinema_id = :cinemaId)
          GROUP BY 1
          ORDER BY 1
      """, nativeQuery = true)
  List<RevenueByDayProjection> getDailyConcessionRevenueInRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("cinemaId") java.util.UUID cinemaId);

  @Query(value = """
          SELECT m.id as id, m.title as title, m.poster_url as posterUrl,
          COUNT(b.id) as tickets, SUM(b.total_amount) as rev
          FROM bookings b
          JOIN showtimes s ON b.showtime_id = s.id
          JOIN screens sc ON s.screen_id = sc.id
          JOIN movies m ON s.movie_id = m.id
          WHERE b.created_at >= :startDate AND b.created_at <= :endDate
            AND b.status IN ('PAID', 'CHECKED_IN', 'EXPIRED')
            AND (:cinemaId IS NULL OR sc.cinema_id = :cinemaId)
          GROUP BY m.id, m.title, m.poster_url
          ORDER BY rev DESC LIMIT 5
      """, nativeQuery = true)
  List<TopMovieProjection> getTop5MoviesInRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("cinemaId") UUID cinemaId);

  @Query(value = """
          SELECT b.id as id, b.booking_code as bookingCode, m.title as movieTitle,
          c.name as cinemaName, s.start_time as startTime, b.total_amount as totalAmount, b.status as status
          FROM bookings b
          JOIN showtimes s ON b.showtime_id = s.id
          JOIN movies m ON s.movie_id = m.id
          JOIN screens sc ON s.screen_id = sc.id
          JOIN cinemas c ON sc.cinema_id = c.id
          WHERE b.created_at >= :startDate AND b.created_at <= :endDate
            AND (:cinemaId IS NULL OR sc.cinema_id = :cinemaId)
          ORDER BY b.created_at DESC LIMIT 5
      """, nativeQuery = true)
  List<RecentBookingProjection> getRecentBookingsInRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("cinemaId") UUID cinemaId);

  // ── Projections ───────────────────────────────────────────────────────

  interface RevenueByDayProjection {
    Object getDate();

    BigDecimal getRevenue();

    Long getBookingCount();
  }

  interface TopMovieProjection {
    String getId();

    String getTitle();

    String getPosterUrl();

    Long getTickets();

    BigDecimal getRev();
  }

  interface RecentBookingProjection {
    String getId();

    String getBookingCode();

    String getMovieTitle();

    String getCinemaName();

    LocalDateTime getStartTime();

    BigDecimal getTotalAmount();

    String getStatus();
  }

  interface RevenueBreakdownProjection {
    String getName();

    BigDecimal getGrossRevenue();
  }
}
