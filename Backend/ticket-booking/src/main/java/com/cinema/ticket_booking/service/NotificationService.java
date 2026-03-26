package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    PageResponse<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable);

    long countUnread(UUID userId);

    void markAllAsRead(UUID userId);

    void sendBookingConfirm(Booking booking);

    void sendPromotion(User user, String title, String body, UUID refId);

    void sendTestNotification(User user);
}