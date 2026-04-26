package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.model.NotificationCampaign;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.time.LocalDateTime;

public interface NotificationService {

    PageResponse<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable);

    long countUnread(UUID userId);

    void markAllAsRead(UUID userId);

    void delete(UUID id, UUID userId);

    void sendBookingConfirm(Booking booking);

    void sendPromotion(User user, String title, String body, UUID refId);


    /**
     * Gửi thông báo cho toàn bộ người dùng qua FCM Topic và lưu vào
     * GlobalNotification.
     */
    void broadcastGlobalNotification(String title, String body, NotificationType type, UUID targetId, String topic);

    /**
     * Tạo một chiến dịch thông báo để gửi ngay hoặc hẹn giờ.
     */
    NotificationCampaign createCampaign(String title, String body, NotificationType type, UUID targetId, String segment,
            LocalDateTime scheduledAt, User creator);

    /**
     * Gửi Push Notification tới một FCM Topic (Data Messaging only).
     */
    void pushToTopic(String topic, String title, String body, NotificationType type, String targetId);
}
