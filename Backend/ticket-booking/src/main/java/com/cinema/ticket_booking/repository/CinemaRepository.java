package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    // Lấy tất cả rạp đang hoạt động
    List<Cinema> findByIsActiveTrue();

    // Lọc theo thành phố
    List<Cinema> findByCityAndIsActiveTrue(String city);

    // Tìm kiếm theo tên (không phân biệt hoa thường)
    List<Cinema> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
}
