package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

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
}
