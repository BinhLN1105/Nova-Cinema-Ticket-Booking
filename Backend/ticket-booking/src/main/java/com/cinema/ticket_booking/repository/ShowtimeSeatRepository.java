package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.ShowtimeSeat;
import com.cinema.ticket_booking.enums.SeatStatus;
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
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {

    // Lấy toàn bộ ghế của 1 suất chiếu (để render sơ đồ ghế)
    @Query("""
        SELECT ss FROM ShowtimeSeat ss
        JOIN FETCH ss.seat s
        WHERE ss.showtime.id = :showtimeId
        ORDER BY s.rowLabel, s.colNumber
    """)
    List<ShowtimeSeat> findByShowtimeIdWithSeat(@Param("showtimeId") UUID showtimeId);

    // Lấy ghế cụ thể trong 1 suất (dùng khi user chọn ghế — cần @Lock)
    @Query("""
        SELECT ss FROM ShowtimeSeat ss
        WHERE ss.showtime.id = :showtimeId
          AND ss.seat.id = :seatId
    """)
    Optional<ShowtimeSeat> findByShowtimeAndSeat(
        @Param("showtimeId") UUID showtimeId,
        @Param("seatId") UUID seatId
    );

    // Lấy nhiều ghế theo danh sách ID (dùng khi user chọn nhiều ghế cùng lúc)
    @Query("""
        SELECT ss FROM ShowtimeSeat ss
        WHERE ss.showtime.id = :showtimeId
          AND ss.id IN :seatIds
    """)
    List<ShowtimeSeat> findByShowtimeAndIds(
        @Param("showtimeId") UUID showtimeId,
        @Param("seatIds") List<UUID> seatIds
    );

    // Đếm số ghế còn trống
    long countByShowtimeIdAndStatus(UUID showtimeId, SeatStatus status);

    // ── Scheduler: reset ghế LOCKED hết hạn về AVAILABLE ─────────────────
    @Modifying
    @Query("""
        UPDATE ShowtimeSeat ss
        SET ss.status    = 'AVAILABLE',
            ss.lockedBy  = NULL,
            ss.lockedUntil = NULL
        WHERE ss.status = 'LOCKED'
          AND ss.lockedUntil < :now
    """)
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    // Xoá toàn bộ ghế của suất chiếu khi admin xoá suất chiếu
    @Modifying
    void deleteByShowtimeId(UUID showtimeId);
}