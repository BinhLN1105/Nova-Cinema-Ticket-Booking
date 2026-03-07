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
}
