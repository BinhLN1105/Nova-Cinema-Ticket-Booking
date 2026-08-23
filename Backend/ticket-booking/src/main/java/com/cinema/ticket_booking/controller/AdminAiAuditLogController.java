package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.AiAuditLogResponse;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.CustomerSearchResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.AiAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai-audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminAiAuditLogController {

    private final AiAuditLogService aiAuditLogService;

    /**
     * GET /api/v1/admin/ai-audit-logs/customers/search?query={keyword}&page=0&size=20
     * Bước 1 trong quy trình CSKH: Tìm kiếm khách hàng theo Tên, Email hoặc SĐT.
     * Response chỉ chứa thông tin định danh tối thiểu, tuyệt đối không lộ nội dung chat.
     */
    @GetMapping("/customers/search")
    public ResponseEntity<ApiResponse<PageResponse<CustomerSearchResponse>>> searchCustomers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        int cappedSize = Math.min(Math.max(size, 1), 20);
        String ipAddress = extractClientIp(request);
        PageResponse<CustomerSearchResponse> results = aiAuditLogService.searchCustomers(
                query,
                currentUser.getId(),
                currentUser.getRole(),
                ipAddress,
                PageRequest.of(page, cappedSize));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/admin/ai-audit-logs/session/{sessionId}
     * Bước 2: Tra cứu toàn bộ lịch sử tin nhắn của một phiên hội thoại cụ thể.
     * Role STAFF bắt buộc truyền reason >= 10 ký tự. Dữ liệu được giải mã AES-256-GCM trong suốt.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<PageResponse<AiAuditLogResponse>>> getLogsBySession(
            @PathVariable String sessionId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        int cappedSize = Math.min(Math.max(size, 1), 50);
        String ipAddress = extractClientIp(request);
        PageResponse<AiAuditLogResponse> logs = aiAuditLogService.getLogsBySession(
                sessionId,
                currentUser.getId(),
                currentUser.getRole(),
                reason,
                ticketId,
                ipAddress,
                PageRequest.of(page, cappedSize));

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * GET /api/v1/admin/ai-audit-logs/user/{userId}
     * Bước 2: Tra cứu toàn bộ lịch sử tương tác AI của một khách hàng cụ thể.
     * Role STAFF bắt buộc truyền reason và bị giới hạn tối đa 30 ngày gần nhất.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<AiAuditLogResponse>>> getLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        int cappedSize = Math.min(Math.max(size, 1), 50);
        String ipAddress = extractClientIp(request);
        PageResponse<AiAuditLogResponse> logs = aiAuditLogService.getLogsByUser(
                userId,
                currentUser.getId(),
                currentUser.getRole(),
                reason,
                ticketId,
                ipAddress,
                PageRequest.of(page, cappedSize));

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
