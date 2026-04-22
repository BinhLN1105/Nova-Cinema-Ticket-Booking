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
    @Modifying(clearAutomatically = true)
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

    // ── Voucher Lazy Release: check if a voucher is TRULY locked ─────────
    boolean existsByUserIdAndVoucherIdAndStatusAndExpiresAtAfter(
            UUID userId,
            UUID voucherId,
            BookingStatus status,
            LocalDateTime now);

    // ── Dashboard Admin Queries ──────────────────────────────────────────

    @Query(value = "SELECT COALESCE(SUM(b.total_amount), 0) FROM bookings b " +
            "WHERE b.status IN ('PAID', 'CHECKED_IN') " +
            "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
            "AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))", nativeQuery = true)
    BigDecimal calculateNetRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId);

    @Query(value = "SELECT COUNT(b.id) FROM bookings b " +
            "WHERE b.status IN ('PAID', 'CHECKED_IN') " +
            "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
            "AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))", nativeQuery = true)
    long countTotalBookingsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = "SELECT COALESCE(SUM(COALESCE(b.discount_amount, 0) + COALESCE(b.promotion_discount_amount, 0)), 0) FROM bookings b "
            +
            "WHERE b.status IN ('PAID', 'CHECKED_IN') " +
            "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
            "AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))", nativeQuery = true)
    BigDecimal calculateTotalDiscounts(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = "SELECT CAST(s.seat_type AS text) AS name, SUM(bi.seat_price) AS grossRevenue " +
            "FROM booking_items bi " +
            "JOIN bookings b ON bi.booking_id = b.id " +
            "JOIN showtime_seats ss ON bi.showtime_seat_id = ss.id " +
            "JOIN seats s ON ss.seat_id = s.id " +
            "WHERE b.status IN ('PAID', 'CHECKED_IN') " +
            "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
            "AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid)) " +
            "GROUP BY s.seat_type", nativeQuery = true)
    List<RevenueBreakdownProjection> getTicketRevenueBySeatType(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = "SELECT c.name AS name, SUM(bc.unit_price * bc.quantity) AS grossRevenue " +
            "FROM booking_combos bc " +
            "JOIN bookings b ON bc.booking_id = b.id " +
            "JOIN combos c ON bc.combo_id = c.id " +
            "WHERE b.status IN ('PAID', 'CHECKED_IN') " +
            "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
            "AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid)) " +
            "GROUP BY c.name", nativeQuery = true)
    List<RevenueBreakdownProjection> getConcessionRevenueByCombo(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT
                    TO_CHAR(b.created_at, 'YYYY-MM-DD') as date,
                    SUM(b.total_amount) as revenue,
                    COUNT(b.id) as bookingCount
                FROM bookings b
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
                GROUP BY 1
                ORDER BY 1
            """, nativeQuery = true)
    List<RevenueByDayProjection> getRevenueByDayInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT
                    TO_CHAR(b.created_at, 'YYYY-MM-DD') as date,
                    SUM(bi.seat_price) as revenue
                FROM bookings b
                JOIN booking_items bi ON bi.booking_id = b.id
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
                GROUP BY 1
                ORDER BY 1
            """, nativeQuery = true)
    List<RevenueByDayProjection> getDailyTicketRevenueInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT
                    TO_CHAR(b.created_at, 'YYYY-MM-DD') as date,
                    SUM(bc.unit_price * bc.quantity) as revenue
                FROM bookings b
                JOIN booking_combos bc ON bc.booking_id = b.id
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
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
                JOIN movies m ON s.movie_id = m.id
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
                GROUP BY m.id, m.title, m.poster_url
                ORDER BY rev DESC LIMIT 5
            """, nativeQuery = true)
    List<TopMovieProjection> getTop5MoviesInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT b.id as id, b.booking_code as bookingCode, 
                       COALESCE(m.title, '🍿 Hóa đơn F&B') as movieTitle,
                       COALESCE(c.name, 'Hệ thống') as cinemaName, 
                       s.start_time as startTime, b.total_amount as totalAmount, b.status as status
                FROM bookings b
                LEFT JOIN showtimes s ON b.showtime_id = s.id
                LEFT JOIN movies m ON s.movie_id = m.id
                LEFT JOIN cinemas c ON b.cinema_id = c.id
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
                ORDER BY b.created_at DESC LIMIT 5
            """, nativeQuery = true)
    List<RecentBookingProjection> getRecentBookingsInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId);

    // ── Analytics Queries ──────────────────────────────────────────

    @Query(value = """
                SELECT EXTRACT(HOUR FROM (CASE WHEN b.showtime_id IS NULL THEN b.created_at ELSE s.start_time END)) as hour,
                       COUNT(b.id) as count
                FROM bookings b
                LEFT JOIN showtimes s ON b.showtime_id = s.id
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid))
                GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> getPeakHoursInRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT c.name, COUNT(ss.id) as total,
                       SUM(CASE WHEN ss.status = 'BOOKED' THEN 1 ELSE 0 END) as booked
                FROM showtime_seats ss
                JOIN showtimes s ON ss.showtime_id = s.id
                JOIN screens sc ON s.screen_id = sc.id
                JOIN cinemas c ON sc.cinema_id = c.id
                WHERE s.start_time >= :startDate AND s.start_time <= :endDate
                  AND (CAST(:cinemaId AS text) IS NULL OR c.id = CAST(:cinemaId AS uuid))
                GROUP BY c.id, c.name
            """, nativeQuery = true)
    List<Object[]> getOccupancyInRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    @Query(value = """
                SELECT COALESCE(c.name, 'Hệ thống / Vãng lai'), SUM(b.total_amount) as revenue, COUNT(b.id) as count
                FROM bookings b
                LEFT JOIN showtimes s ON b.showtime_id = s.id
                LEFT JOIN screens sc ON s.screen_id = sc.id
                LEFT JOIN cinemas c ON (sc.cinema_id = c.id OR b.cinema_id = c.id)
                WHERE b.created_at >= :startDate AND b.created_at <= :endDate
                  AND b.status IN ('PAID', 'CHECKED_IN')
                  AND (CAST(:cinemaId AS text) IS NULL OR b.cinema_id = CAST(:cinemaId AS uuid) OR c.id = CAST(:cinemaId AS uuid))
                GROUP BY c.id, c.name
                ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> getRevenueByCinemaInRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cinemaId") UUID cinemaId);

    // ── Staff Dashboard Search ──────────────────────────────────────────

    @Query("SELECT b FROM Booking b WHERE " +
            "(CAST(:startDate AS LocalDateTime) IS NULL OR b.createdAt >= :startDate) AND " +
            "(CAST(:endDate AS LocalDateTime) IS NULL OR b.createdAt <= :endDate) AND " +
            "(CAST(:cinemaId AS string) IS NULL OR b.cinema.id = :cinemaId) AND " +
            "(CAST(:status AS string) IS NULL OR b.status = :status) AND " +
            "(CAST(:paymentMethod AS string) IS NULL OR b.paymentMethod = :paymentMethod) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "   LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Booking> searchBookings(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cinemaId") UUID cinemaId,
            @Param("status") BookingStatus status,
            @Param("paymentMethod") com.cinema.ticket_booking.enums.PaymentMethod paymentMethod,
            @Param("search") String search,
            Pageable pageable);

    // ── Staff: Lịch sử check-in theo rạp (cho màn hình Lịch sử soát vé) ──
    @Query(value = """
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user u
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            LEFT JOIN FETCH b.bookingItems bi
            LEFT JOIN FETCH bi.showtimeSeat ss
            LEFT JOIN FETCH ss.seat
            WHERE b.cinema.id = :cinemaId
              AND b.status = 'CHECKED_IN'
            ORDER BY b.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            WHERE b.cinema.id = :cinemaId
              AND b.status = 'CHECKED_IN'
            """)
    Page<Booking> findCheckedInByCinemaOrderByCreatedAtDesc(
            @Param("cinemaId") UUID cinemaId,
            Pageable pageable);

    List<Booking> findTop20ByCinemaIdAndStatusOrderByCreatedAtDesc(UUID cinemaId, BookingStatus status);

    // ── Projections ───────────────────────────────────────────────────────

    interface RevenueByDayProjection {
        String getDate();
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
