package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.mapper.NotificationMapper;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        return PageResponse.of(
                notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable)
                        .map(notificationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ── Gửi thông báo nội bộ (gọi từ các Service khác) ───────────────────

    @Override
    public void sendBookingConfirm(Booking booking) {
        send(
                booking.getUser(),
                "Đặt vé thành công! 🎬",
                "Đơn " + booking.getBookingCode() + " - " + booking.getShowtime().getMovie().getTitle()
                        + " đã được xác nhận. Kiểm tra QR code trong ứng dụng.",
                NotificationType.BOOKING_CONFIRM,
                booking.getId());
        pushFcm(booking.getUser(), "Đặt vé thành công! 🎬",
                "Phim " + booking.getShowtime().getMovie().getTitle());
    }

    @Override
    public void sendPromotion(User user, String title, String body, UUID refId) {
        send(user, title, body, NotificationType.PROMOTION, refId);
        pushFcm(user, title, body);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private void send(User user, String title, String body, NotificationType type, UUID refId) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .refId(refId)
                .isRead(false)
                .build());
    }

    /**
     * Gửi Firebase Cloud Messaging.
     * TODO: tích hợp firebase-admin SDK
     */
    private void pushFcm(User user, String title, String body) {
        if (user.getFcmToken() == null)
            return;
        log.info("[FCM] → {} | {} | {}", user.getEmail(), title, body);
        // FirebaseMessaging.getInstance().send(...)
    }
}
