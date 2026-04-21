package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Lấy tất cả ticket của 1 booking (dùng cho check-in: mark từng ghế)
    List<Ticket> findByBookingId(UUID bookingId);

    // Lấy ticket kèm thông tin ghế để hiển thị chi tiết
    @Query("""
        SELECT t FROM Ticket t
        JOIN FETCH t.bookingItem bi
        JOIN FETCH bi.showtimeSeat ss
        JOIN FETCH ss.seat s
        WHERE t.booking.id = :bookingId
        ORDER BY s.rowLabel, s.colNumber
    """)
    List<Ticket> findByBookingIdWithSeatDetail(@Param("bookingId") UUID bookingId);

    // Kiểm tra toàn bộ ghế trong booking đã check-in chưa
    @Query("""
        SELECT COUNT(t) = 0 FROM Ticket t
        WHERE t.booking.id = :bookingId
          AND t.isUsed = false
    """)
    boolean areAllTicketsUsed(@Param("bookingId") UUID bookingId);

    // Đếm vé đã soát của một rạp trong khoảng thời gian (dùng cho Staff Dashboard)
    @Query("""
        SELECT COUNT(t) FROM Ticket t
        JOIN t.bookingItem bi
        JOIN bi.showtimeSeat ss
        JOIN ss.showtime st
        WHERE st.screen.cinema.id = :cinemaId
          AND t.isUsed = true
          AND t.usedAt >= :from
          AND t.usedAt < :to
    """)
    long countCheckedInByCinemaAndDateRange(
            @Param("cinemaId") UUID cinemaId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}