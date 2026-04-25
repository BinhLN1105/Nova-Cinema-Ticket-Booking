package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    // Lấy toàn bộ ghế của một phòng (để vẽ sơ đồ ghế)
    List<Seat> findByScreenIdAndIsActiveTrueOrderByRowLabelAscColNumberAsc(UUID screenId);

    // Lấy ghế theo loại trong một phòng
    List<Seat> findByScreenIdAndSeatTypeAndIsActiveTrue(UUID screenId, SeatType seatType);

    // Đếm số ghế active trong phòng
    long countByScreenIdAndIsActiveTrue(UUID screenId);
}

