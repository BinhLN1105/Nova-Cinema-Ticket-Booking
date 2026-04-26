package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    // Lấy tất cả rạp đang hoạt động
    List<Cinema> findByIsActiveTrue();

    // Lọc theo thành phố hoặc địa chỉ (không phân biệt hoa thường)
    @Query("SELECT c FROM Cinema c WHERE " +
            "(LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.address) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "c.isActive = true")
    List<Cinema> searchCinemas(@Param("query") String query);

    // Tìm kiếm theo tên (không phân biệt hoa thường)
    List<Cinema> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
}
