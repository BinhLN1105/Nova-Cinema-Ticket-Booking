package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    // Lấy tất cả phòng chiếu của một rạp
    List<Screen> findByCinemaIdAndIsActiveTrue(UUID cinemaId);
}
