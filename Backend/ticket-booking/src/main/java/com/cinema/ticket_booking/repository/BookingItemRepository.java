package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    // Lấy tất cả ghế trong 1 đơn (kèm thông tin ghế để hiển thị)
    @Query("""
                SELECT bi FROM BookingItem bi
                JOIN FETCH bi.showtimeSeat ss
                JOIN FETCH ss.seat s
                LEFT JOIN FETCH bi.ticket t
                WHERE bi.booking.id = :bookingId
                ORDER BY s.rowLabel, s.colNumber
            """)
    List<BookingItem> findByBookingIdWithSeat(@Param("bookingId") UUID bookingId);

    // Lấy tất cả ghế của danh sách đơn (gom nhóm cho load danh sách để tránh N+1 query)
    @Query("""
                SELECT bi FROM BookingItem bi
                JOIN FETCH bi.showtimeSeat ss
                JOIN FETCH ss.seat s
                LEFT JOIN FETCH bi.ticket t
                WHERE bi.booking.id IN :bookingIds
                ORDER BY s.rowLabel, s.colNumber
            """)
    List<BookingItem> findByBookingIdInWithSeat(@Param("bookingIds") List<UUID> bookingIds);
}
