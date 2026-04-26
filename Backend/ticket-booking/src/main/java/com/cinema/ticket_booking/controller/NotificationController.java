package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications?page=0&size=20
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getMyNotifications(currentUser.getId(), pageable)));
    }

    // GET /api/v1/notifications/unread-count — số thông báo chưa đọc (badge)
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        long count = notificationService.countUnread(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    // PATCH /api/v1/notifications/read-all — đánh dấu đọc tất cả
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đánh dấu đọc tất cả"));
    }


    // DELETE /api/v1/notifications/{id} — xóa thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        notificationService.delete(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa thông báo"));
    }
}
