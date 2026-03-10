package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Tìm theo mã booking (dùng cho check-in QR)
    Optional<Booking> findByBookingCode(String bookingCode);

    // Tìm theo QR code string
    Optional<Booking> findByQrCode(String qrCode);

    // Lịch sử đặt vé của user (phân trang)
    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Đơn đặt vé của user theo trạng thái
    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    // Admin: xem booking theo suất chiếu
    List<Booking> findByShowtimeId(UUID showtimeId);

    // ── Scheduler: tự động EXPIRED booking PENDING quá hạn ───────────────
    @Modifying
    @Query("""
                UPDATE Booking b
                SET b.status = 'EXPIRED'
                WHERE b.status = 'PENDING'
                  AND b.expiresAt < :now
            """)
    int expireOverdueBookings(@Param("now") LocalDateTime now);

    // Kiểm tra user đã có booking PAID cho suất chiếu chưa (để cho phép review)
    @Query("""
                SELECT COUNT(b) > 0 FROM Booking b
                WHERE b.user.id     = :userId
                  AND b.showtime.movie.id = :movieId
                  AND b.status      = 'PAID'
            """)
    boolean hasUserPaidForMovie(
            @Param("userId") UUID userId,
            @Param("movieId") UUID movieId);

    // ── Dashboard Admin Queries ──────────────────────────────────────────

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM bookings WHERE status = 'COMPLETED'", nativeQuery = true)
    java.math.BigDecimal calculateTotalRevenue();

    @Query(value = "SELECT COUNT(*) FROM bookings WHERE status = 'COMPLETED'", nativeQuery = true)
    long countTotalBookings();

    @Query(value = "SELECT DATE(created_at) as date, SUM(total_amount) as revenue " +
            "FROM bookings " +
            "WHERE status = 'COMPLETED' AND created_at >= CURRENT_DATE - INTERVAL '6 days' " +
            "GROUP BY DATE(created_at) ORDER BY date ASC", nativeQuery = true)
    List<RevenueByDayProjection> getRevenueByDay();

    @Query(value = "SELECT m.id as id, m.title as title, m.poster_url as posterUrl, " +
            "COUNT(b.id) as tickets, SUM(b.total_amount) as rev " +
            "FROM bookings b " +
            "JOIN showtimes s ON b.showtime_id = s.id " +
            "JOIN movies m ON s.movie_id = m.id " +
            "WHERE b.status = 'COMPLETED' " +
            "GROUP BY m.id, m.title, m.poster_url " +
            "ORDER BY rev DESC LIMIT 5", nativeQuery = true)
    List<TopMovieProjection> getTop5Movies();

    @Query(value = "SELECT b.id as id, b.booking_code as bookingCode, m.title as movieTitle, " +
            "c.name as cinemaName, s.start_time as startTime, b.total_amount as totalAmount, b.status as status " +
            "FROM bookings b " +
            "JOIN showtimes s ON b.showtime_id = s.id " +
            "JOIN movies m ON s.movie_id = m.id " +
            "JOIN screens sc ON s.screen_id = sc.id " +
            "JOIN cinemas c ON sc.cinema_id = c.id " +
            "ORDER BY b.created_at DESC LIMIT 5", nativeQuery = true)
    List<RecentBookingProjection> getRecentBookings();

    // ── Projections ───────────────────────────────────────────────────────

    interface RevenueByDayProjection {
        String getDate();
        java.math.BigDecimal getRevenue();
    }

    interface TopMovieProjection {
        String getId();
        String getTitle();
        String getPosterUrl();
        Long getTickets();
        java.math.BigDecimal getRev();
    }

    interface RecentBookingProjection {
        String getId();
        String getBookingCode();
        String getMovieTitle();
        String getCinemaName();
        java.sql.Timestamp getStartTime();
        java.math.BigDecimal getTotalAmount();
        String getStatus();
    }
}
