package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Lấy tất cả thông báo của user (mới nhất trước, phân trang)
    Page<Notification> findByUserIdOrderBySentAtDesc(UUID userId, Pageable pageable);

    // Đếm thông báo chưa đọc (hiển thị badge trên app)
    long countByUserIdAndIsReadFalse(UUID userId);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);

    // Lấy danh sách thông báo theo phân loại của user (mới nhất trước)
    List<Notification> findByUserIdAndTypeOrderBySentAtDesc(UUID userId,
            NotificationType type);

    // Xóa tất cả thông báo theo phân loại của user
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.type = :type")
    void deleteByUserIdAndType(@Param("userId") UUID userId,
            @Param("type") NotificationType type);
}
