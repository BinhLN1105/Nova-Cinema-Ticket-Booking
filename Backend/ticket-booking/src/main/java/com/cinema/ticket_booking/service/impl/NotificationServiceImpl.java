package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.GlobalNotification;
import com.cinema.ticket_booking.model.NotificationCampaign;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.enums.CampaignStatus;
import com.cinema.ticket_booking.mapper.NotificationMapper;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.GlobalNotificationRepository;
import com.cinema.ticket_booking.repository.NotificationCampaignRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final GlobalNotificationRepository globalNotificationRepository;
    private final NotificationCampaignRepository notificationCampaignRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        // 1. Lấy thông báo cá nhân (paged)
        var personalPage = notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
        List<NotificationResponse> result = new ArrayList<>(personalPage.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList()));

        // 2. Chỉ ở trang đầu tiên: Lấy thêm các thông báo Global còn hạn
        // Chú ý: Thực tế nên làm Topic subscription ở App để biết user thuộc Topic nào.
        // Ở đây ta mặc định user nhận topic "nova_all_users" và kiểm tra tier nếu là
        // VIP.
        if (pageable.getPageNumber() == 0) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                List<String> topics = new ArrayList<>();
                topics.add("nova_all_users");
                if (user.getMembershipTier() == MembershipTier.GOLD ||
                        user.getMembershipTier() == MembershipTier.DIAMOND) {
                    topics.add("nova_vip_users");
                }

                List<GlobalNotification> globalList = globalNotificationRepository.findActiveByTopics(topics,
                        LocalDateTime.now());
                result.addAll(globalList.stream()
                        .map(notificationMapper::toResponse)
                        .collect(Collectors.toList()));

                // Sắp xếp lại theo thời gian
                result.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));
            }
        }

        return PageResponse.of(personalPage, result);
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
        pushFcm(user, "Thông báo kiểm tra! 🎬", "FCM đã được cấu hình thành công trên thiết bị của bạn.", null);
    }

    // ── Broadcast & Campaigns ───────────────────────────────────────────

    @Override
    public void broadcastGlobalNotification(String title, String body, NotificationType type, UUID targetId,
            String topic) {
        // 1. Lưu vào Database (Persistence)
        GlobalNotification gn = GlobalNotification.builder()
                .title(title)
                .body(body)
                .type(type)
                .targetId(targetId)
                .targetTopic(topic)
                .expiresAt(LocalDateTime.now().plusDays(30)) // Mặc định 30 ngày
                .build();
        globalNotificationRepository.save(gn);

        // 2. Bắn qua FCM Topic
        pushToTopic(topic, title, body, type, targetId != null ? targetId.toString() : null);
    }

    @Override
    public NotificationCampaign createCampaign(String title, String body, NotificationType type, UUID targetId,
            String segment, LocalDateTime scheduledAt, User creator) {
        NotificationCampaign campaign = NotificationCampaign.builder()
                .title(title)
                .body(body)
                .type(type)
                .targetId(targetId)
                .targetTopic(segment)
                .scheduledAt(scheduledAt)
                .status(CampaignStatus.PENDING)
                .createdBy(creator)
                .build();
        return notificationCampaignRepository.save(campaign);
    }

    @Override
    @Async
    public void pushToTopic(String topic, String title, String body, NotificationType type, String targetId) {
        // Data Message only for total control
        Message message = Message.builder()
                .setTopic(topic)
                .putData("title", title)
                .putData("body", body)
                .putData("type", type.name())
                .putData("targetId", targetId != null ? targetId : "")
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM Topic] ✓ {} → {}", topic, response);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM Topic] ✗ {} → {}", topic, e.getMessage());
        }
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
        if (!user.getAllowTransactionNotification()) {
            log.info("[FCM] Skipped for {} due to user preference (Transactions OFF)", user.getEmail());
            return;
        }
        if (user.getFcmToken() == null || user.getFcmToken().isBlank())
            return;

        // Data Message only (Android Native) - No Notification block
        Message message = Message.builder()
                .setToken(user.getFcmToken())
                .putData("title", title)
                .putData("body", body)
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
