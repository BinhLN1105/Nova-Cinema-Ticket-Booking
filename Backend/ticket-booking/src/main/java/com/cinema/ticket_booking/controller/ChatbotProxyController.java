package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.service.AiChatService;
import com.cinema.ticket_booking.service.AiAuditLogService;
import com.cinema.ticket_booking.service.JwtService;
import com.cinema.ticket_booking.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * ChatbotProxyController.java
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * Proxy layer: Frontend → Java → Python RAG server
 *
 * Lợi ích:
 * - Frontend chỉ biết 1 server (Java), không biết Python tồn tại
 * - Java kiểm soát auth/session trước khi chuyển sang Python
 * - Dễ thêm rate limiting, logging, abuse detection
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotProxyController {

    private final AiChatService aiChatService;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final AiAuditLogService aiAuditLogService;

    /**
     * POST /api/v1/chatbot/chat
     * Frontend gọi endpoint này (đã xác thực JWT)
     */
    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(
            @RequestBody ChatRequest req,
            HttpServletRequest httpReq) {

        // 1. Lấy userId từ JWT
        String token = extractToken(httpReq);
        String userIdStr = jwtService.extractUserId(token);
        UUID userId = UUID.fromString(userIdStr);

        // Lớp 2: Rate limit per-user - Giới hạn 10 tin nhắn/phút
        String rateLimitKey = "ai_rate_limit:user:" + userId;
        Long userCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (userCount != null && userCount == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }
        if (userCount != null && userCount > 10) {
            log.warn("[Chatbot Rate Limit] UserId {} exceeded limit (count: {})", userId, userCount);
            throw new AppException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đang nhắn tin quá nhanh. Vui lòng thử lại sau giây lát!");
        }

        // Lớp 0: Session-to-User Mapping (sinh uuid ngẫu nghiên, cache Redis)
        String userSessionKey = "ai_user_session:" + userId;
        String sessionId = redisTemplate.opsForValue().get(userSessionKey);

        if (sessionId == null) {
            sessionId = "session_" + UUID.randomUUID().toString();
            // Lưu chéo Token -> User ID và User ID -> Token; TTL = 30 phút
            redisTemplate.opsForValue().set("ai_session_user:" + sessionId, userIdStr, Duration.ofMinutes(30));
            redisTemplate.opsForValue().set(userSessionKey, sessionId, Duration.ofMinutes(30));
        } else {
            // Gia hạn TTL cho session khi có hoạt động
            redisTemplate.expire("ai_session_user:" + sessionId, Duration.ofMinutes(30));
            redisTemplate.expire(userSessionKey, Duration.ofMinutes(30));
        }

        log.info("[Chatbot] userId={} | sessionId={} | msg={}", userId, sessionId,
                req.userMessage().substring(0, Math.min(50, req.userMessage().length())));

        // 2. Chuyển tiếp sang Python RAG sever và nhận kết quả Map cấu trúc
        Map<String, Object> aiResult = aiChatService.sendMessage(sessionId, req.userMessage());
        String reply = (String) aiResult.getOrDefault("reply", "");
        String intent = (String) aiResult.getOrDefault("intent", "UNKNOWN");
        boolean usedFallback = (Boolean) aiResult.getOrDefault("used_fallback", false);

        // Lớp 6: Ghi nhận nhật ký kiểm toán (Audit Logging) - Bất đồng bộ
        try {
            aiAuditLogService.logInteractionAsync(userId, sessionId, req.userMessage(), reply, intent, usedFallback);
        } catch (Exception e) {
            log.error("[Chatbot Audit Log] Không thể ghi nhận log: {}", e.getMessage());
        }

        return ApiResponse.success(Map.of(
                "reply", reply,
                "session_id", sessionId,
                "used_fallback", usedFallback));
    }

    /**
     * POST /api/v1/chatbot/session/clear
     * Xóa lịch sử hội thoại khi user bắt đầu cuộc trò chuyện mới
     */
    @PostMapping("/session/clear")
    public ApiResponse<Map<String, String>> clearSession(HttpServletRequest httpReq) {
        String token = extractToken(httpReq);
        String userIdStr = jwtService.extractUserId(token);

        String userSessionKey = "ai_user_session:" + userIdStr;
        String sessionId = redisTemplate.opsForValue().get(userSessionKey);

        if (sessionId != null) {
            // Xóa session cả 2 chiều trong Redis
            redisTemplate.delete("ai_session_user:" + sessionId);
            redisTemplate.delete(userSessionKey);
            aiChatService.clearSession(sessionId);
        }

        return ApiResponse.success(Map.of("status", "ok"));
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new RuntimeException("Missing Authorization header");
    }

    record ChatRequest(String userMessage) {
    }
}
