package com.cinema.ticket_booking.controller.ai;

import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.enums.ReminderType;
import com.cinema.ticket_booking.exception.AppException;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AiAgentController.java
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * Controller nội bộ phục vụ Python AI Agent.
 * Bảo mật: X-Internal-Key & Session-to-User Mapping trong Redis.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@RestController
@RequestMapping("/internal/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiAgentController {

    @Value("${nova.internal.api-key}")
    private String internalApiKey;

    private final BookingService bookingService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ShowtimeService showtimeService;

    // ── Security & Authentication ──────────────────────────
    private void validateKey(String key) {
        if (!internalApiKey.equals(key)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }

    private UUID resolveUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing session ID");
        }
        String userIdStr = redisTemplate.opsForValue().get("ai_session_user:" + sessionId);
        if (userIdStr == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Session expired or invalid");
        }
        return UUID.fromString(userIdStr);
    }

    // ── Endpoints ───────────────────────────────────────────

    /**
     * POST /internal/api/ai/booking/draft
     * Tạo báo giá giữ chỗ ảo và cache vào Redis làm draft booking.
     */
    @PostMapping("/booking/draft")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDraftBooking(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody AiDraftBookingRequest req) throws JsonProcessingException {

        validateKey(key);
        UUID userId = resolveUser(sessionId);

        log.info("[AI Draft Booking] Session {} (UserId: {}) requesting showtime={}", sessionId, userId,
                req.getShowtimeId());

        // 1. Kiểm tra hạn mức ghế: tối đa 8 ghế
        if (req.getSeatIds() == null || req.getSeatIds().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Danh sách ghế ngồi không được để trống");
        }
        if (req.getSeatIds().size() > 8) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn chỉ được đặt tối đa 8 ghế một lần qua AI.");
        }

        // 2. Kiểm tra hạn mức combo: tổng số combo tối đa 10 phần
        int totalComboQty = 0;
        if (req.getCombos() != null) {
            totalComboQty = req.getCombos().stream().mapToInt(ComboRequestItem::getQuantity).sum();
            if (totalComboQty > 10) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Bạn chỉ được đặt tối đa 10 phần combo một lần qua AI.");
            }
        }

        // 3. Tạo request báo giá thực tế
        BookingRequest quoteRequest = new BookingRequest();
        quoteRequest.setShowtimeId(req.getShowtimeId());
        quoteRequest.setShowtimeSeatIds(req.getSeatIds());
        if (req.getCombos() != null) {
            List<BookingRequest.ComboItem> bookingCombos = req.getCombos().stream()
                    .map(c -> {
                        BookingRequest.ComboItem item = new BookingRequest.ComboItem();
                        item.setComboId(c.getComboId());
                        item.setQuantity(c.getQuantity());
                        return item;
                    }).toList();
            quoteRequest.setCombos(bookingCombos);
        }

        // 4. Lấy thông tin báo giá (xác thực số tiền, ngày chiếu, loại ghế)
        BookingResponse quoteResponse = bookingService.calculateQuote(userId, quoteRequest);

        // 5. Sinh draftId ngẫu nhiên (Java kiểm soát sinh ID)
        String draftId = UUID.randomUUID().toString();

        // 6. Cache nội dung vào Redis với TTL 10 phút (600s)
        DraftBookingCache draftCache = DraftBookingCache.builder()
                .userId(userId)
                .showtimeId(req.getShowtimeId())
                .showtimeSeatIds(req.getSeatIds())
                .combos(req.getCombos())
                .build();

        String serializedDraft = objectMapper.writeValueAsString(draftCache);
        redisTemplate.opsForValue().set("ai_draft_booking:" + draftId, serializedDraft, Duration.ofMinutes(10));

        // 7. Trả về thông số chi tiết
        Map<String, Object> dataResponse = new HashMap<>();
        dataResponse.put("draftId", draftId);
        dataResponse.put("showtimeId", req.getShowtimeId());
        dataResponse.put("seats", req.getSeatIds());
        dataResponse.put("totalAmount", quoteResponse.getTotalAmount());
        dataResponse.put("movieTitle", quoteResponse.getMovieTitle());
        dataResponse.put("startTime", quoteResponse.getStartTime().toString());
        dataResponse.put("cinemaName", quoteResponse.getCinemaName());

        return ResponseEntity.ok(ApiResponse.success(dataResponse, "Tạo đơn vé nháp thành công"));
    }

    /**
     * POST /internal/api/ai/reminder/draft
     * Ghi nhận thông báo nhắc nhở lịch xem phim, có rate limit 5 lần/ngày/user.
     */
    @PostMapping("/reminder/draft")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDraftReminder(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody AiDraftReminderRequest req) {

        validateKey(key);
        UUID userId = resolveUser(sessionId);

        // Lớp 5: Rate limit reminder 5 lần mỗi ngày
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String limitKey = "ai_reminder_limit:" + userId + ":" + today;

        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(limitKey, Duration.ofDays(1));
        }

        if (count != null && count > 5) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Hạn mức nhắc lịch trong ngày của bạn đã đạt giới hạn (tối đa 5 lần).");
        }

        // Lấy User Entity từ Database
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin cấu hình tài khoản"));

        // Lấy thông tin suất chiếu từ DB để lấy phim & rạp phim thực tế, phòng chống
        // giả mạo thuộc tính
        ShowtimeResponse showtime = showtimeService
                .getById(UUID.fromString(req.getShowtimeId()));

        // Lưu trực tiếp Notification vào Database (Ngoại lệ được kiểm soát)
        LocalDateTime startTime = showtime.getStartTime();
        String timePart = startTime.toLocalTime().toString().substring(0, 5); // "HH:mm"
        String datePart = startTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"));

        String title = "Nhắc lịch xem phim: " + showtime.getMovieTitle();
        String body = "Bạn có lịch hẹn chiếu phim lúc " + timePart + " ngày " + datePart + " tại "
                + showtime.getCinemaName();

        if (req.getReminderType() == ReminderType.BOOKING) {
            title = "Nhắc lịch đặt vé: " + showtime.getMovieTitle();
            body = "Bạn có lịch hẹn đặt vé cho suất chiếu lúc " + timePart + " ngày " + datePart + " tại "
                    + showtime.getCinemaName();
        }

        Notification reminder = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(NotificationType.REMINDER)
                .isRead(false)
                .build();

        notificationRepository.save(reminder);

        log.info("[AI Reminder] Added reminder to user {} for showtime {} | type={} | count={}",
                userId, req.getShowtimeId(), req.getReminderType(), count);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "success", "countToday", count),
                "Đã cài đặt nhắc nhở lịch xem phim thành công"));
    }

    /**
     * GET /internal/api/ai/reminder/list
     * Lấy danh sách nhắc lịch của user hiện tại.
     */
    @GetMapping("/reminder/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReminderList(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId) {
        validateKey(key);
        UUID userId = resolveUser(sessionId);

        List<Notification> reminders = notificationRepository.findByUserIdAndTypeOrderBySentAtDesc(
                userId, NotificationType.REMINDER);

        List<Map<String, Object>> data = reminders.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId().toString());
            map.put("title", r.getTitle());
            map.put("body", r.getBody());
            map.put("createdAt", r.getSentAt() != null ? r.getSentAt().toString() : "");
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách nhắc lịch thành công"));
    }

    /**
     * DELETE /internal/api/ai/reminder/{id}
     * Xóa một nhắc lịch cụ thể của user hiện tại (có xác thực quyền sở hữu chéo).
     */
    @DeleteMapping("/reminder/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> deleteReminder(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable("id") String idStr) {
        validateKey(key);
        UUID userId = resolveUser(sessionId);

        UUID reminderId;
        try {
            reminderId = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "ID nhắc lịch không đúng định dạng UUID");
        }

        Notification reminder = notificationRepository.findById(reminderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Nhắc lịch này đã được xử lý hoặc không còn tồn tại"));

        if (!reminder.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa nhắc lịch này");
        }

        notificationRepository.delete(reminder);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa nhắc lịch thành công"));
    }

    /**
     * DELETE /internal/api/ai/reminder/all
     * Xóa toàn bộ nhắc lịch của user hiện tại.
     */
    @DeleteMapping("/reminder/all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> deleteAllReminders(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId) {
        validateKey(key);
        UUID userId = resolveUser(sessionId);

        notificationRepository.deleteByUserIdAndType(userId, NotificationType.REMINDER);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa toàn bộ nhắc lịch thành công"));
    }

    /**
     * GET /internal/api/ai/user/tickets
     * Tra cứu xem vé đã mua của user dựa trên session ID.
     */
    @GetMapping("/user/tickets")
    public ResponseEntity<ApiResponse<Object>> getUserTickets(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-Session-Id") String sessionId) {

        validateKey(key);
        UUID userId = resolveUser(sessionId);

        log.info("[AI User Tickets] Query tickets list for user={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin cấu hình tài khoản"));

        String rankVi = "Thành viên";
        if (user.getMembershipTier() != null) {
            switch (user.getMembershipTier()) {
                case SILVER -> rankVi = "Bạc";
                case GOLD -> rankVi = "Vàng";
                case DIAMOND -> rankVi = "Kim cương";
                default -> rankVi = "Thành viên";
            }
        }

        var pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        var ticketHistory = bookingService.getMyBookings(userId, pageable);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tickets", ticketHistory.getContent());
        payload.put("cinePoints", user.getRewardPoints());
        payload.put("rank", rankVi);

        return ResponseEntity.ok(ApiResponse.success(payload, "Tra cứu lịch sử vé đặt thành công"));
    }

    // ── DTO Requests and Caches ─────────────────────────────

    @Data
    public static class AiDraftBookingRequest {
        private String showtimeId;
        private List<String> seatIds;
        private List<ComboRequestItem> combos;
    }

    @Data
    public static class ComboRequestItem {
        private String comboId;
        private Integer quantity;
    }

    @Data
    public static class AiDraftReminderRequest {
        private String showtimeId;
        private ReminderType reminderType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DraftBookingCache {
        private UUID userId;
        private String showtimeId;
        private List<String> showtimeSeatIds;
        private List<ComboRequestItem> combos;
    }
}
