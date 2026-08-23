package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.mapper.NotificationMapper;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GlobalNotificationRepository globalNotificationRepository;
    @Mock
    private NotificationCampaignRepository notificationCampaignRepository;
    @Mock
    private UserHiddenNotificationRepository userHiddenNotificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private User user;
    private Notification notification;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("user@gmail.com").build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Đặt vé thành công")
                .body("Bạn đã đặt vé xem phim Mai thành công.")
                .type(NotificationType.BOOKING_CONFIRM)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        notificationResponse = NotificationResponse.builder()
                .id(notification.getId().toString())
                .title("Đặt vé thành công")
                .body("Bạn đã đặt vé xem phim Mai thành công.")
                .type(NotificationType.BOOKING_CONFIRM)
                .isRead(false)
                .sentAt(notification.getSentAt())
                .build();
    }

    @Test
    @DisplayName("1. getMyNotifications lấy danh sách thông báo thành công")
    void testGetMyNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(notificationRepository.findByUserIdOrderBySentAtDesc(eq(userId), any(Pageable.class))).thenReturn(page);
        when(notificationMapper.toResponse(notification)).thenReturn(notificationResponse);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userHiddenNotificationRepository.findHiddenGlobalNotificationIdsByUserId(userId)).thenReturn(Collections.emptyList());
        when(globalNotificationRepository.findActiveByTopics(anyList(), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(userId, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Đặt vé thành công", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("2. countUnread đếm số thông báo chưa đọc")
    void testCountUnread() {
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);
        long unread = notificationService.countUnread(userId);
        assertEquals(5L, unread);
    }

    @Test
    @DisplayName("3. markAllAsRead đánh dấu tất cả đã đọc")
    void testMarkAllAsRead() {
        notificationService.markAllAsRead(userId);
        verify(notificationRepository, times(1)).markAllAsRead(userId);
    }
}
