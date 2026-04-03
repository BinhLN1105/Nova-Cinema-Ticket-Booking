package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    // Cho người dùng (Chỉ lấy phòng đang hoạt động và chưa bị xóa)
    List<Screen> findByCinemaIdAndIsActiveTrueAndIsDeletedFalse(UUID cinemaId);

    // Cho Admin (Lấy tất cả phòng chiếu chưa bị xóa, bao gồm cả phòng đang bảo trì)
    List<Screen> findByCinemaIdAndIsDeletedFalse(UUID cinemaId);

    // Tính số lượng phòng chiếu của rạp (Để kiểm tra trước khi xóa)
    long countByCinemaIdAndIsDeletedFalse(UUID cinemaId);
}
