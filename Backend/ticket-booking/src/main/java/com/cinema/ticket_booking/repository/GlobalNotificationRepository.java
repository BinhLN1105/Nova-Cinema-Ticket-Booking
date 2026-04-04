package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.GlobalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GlobalNotificationRepository extends JpaRepository<GlobalNotification, UUID> {

    /**
     * Tìm các thông báo toàn hệ thống còn hiệu lực và khớp với các topic của user.
     */
    @Query("SELECT gn FROM GlobalNotification gn " +
           "WHERE gn.targetTopic IN :topics " +
           "AND (gn.expiresAt IS NULL OR gn.expiresAt > :now) " +
           "ORDER BY gn.sentAt DESC")
    List<GlobalNotification> findActiveByTopics(
            @Param("topics") List<String> topics,
            @Param("now") LocalDateTime now);
}
