package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.mapper.NotificationMapper;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.NotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
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
    private final UserRepository userRepository;

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
                "Phim " + booking.getShowtime().getMovie().getTitle(),
                booking.getId().toString());
    }

    @Override
    public void sendPromotion(User user, String title, String body, UUID refId) {
        send(user, title, body, NotificationType.PROMOTION, refId);
        pushFcm(user, title, body, refId != null ? refId.toString() : "");
    }

    @Override
    public void sendTestNotification(User user) {
        pushFcm(user, "Thông báo kiểm tra! 🎬", "FCM đã được cấu hình thành công trên trình duyệt của bạn.", null);
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
     * Gửi Firebase Cloud Messaging push notification.
     * Chạy async để không block luồng chính.
     * Dùng clearFcmToken() thay vì save(user) để tránh detached entity ghi đè data.
     */
    @Async
    public void pushFcm(User user, String title, String body, String bookingId) {
        if (user.getFcmToken() == null || user.getFcmToken().isBlank())
            return;

        // Notification + Data payload (cho Deep Linking trên Android)
        Message message = Message.builder()
                .setToken(user.getFcmToken())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", "BOOKING_REMINDER")
                .putData("targetId", bookingId != null ? bookingId : "")
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] ✓ {} → {}", user.getEmail(), response);
        } catch (FirebaseMessagingException e) {
            log.warn("[FCM] ✗ {} → {}", user.getEmail(), e.getMessage());
            // Token không hợp lệ → xoá bằng custom query (tránh detached entity)
            if ("UNREGISTERED".equals(e.getMessagingErrorCode().name())
                    || "INVALID_ARGUMENT".equals(e.getMessagingErrorCode().name())) {
                userRepository.clearFcmToken(user.getEmail());
                log.info("[FCM] Cleared stale token for {}", user.getEmail());
            }
        }
    }
}
