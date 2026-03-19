package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.service.AiChatService;
import com.cinema.ticket_booking.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * POST /api/v1/chatbot/chat
     * Frontend gọi endpoint này (đã xác thực JWT)
     */
    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(
            @RequestBody ChatRequest req,
            HttpServletRequest httpReq) {
        // 1. Lấy userId từ JWT để tạo session_id unique per user
        String token = extractToken(httpReq);
        String userId = jwtService.extractUserId(token);
        String sessionId = "user_" + userId; // mỗi user 1 session

        log.info("[Chatbot] userId={} | msg={}", userId,
                req.userMessage().substring(0, Math.min(50, req.userMessage().length())));

        // 2. Forward sang Python RAG server
        String reply = aiChatService.sendMessage(sessionId, req.userMessage());
        
        return ApiResponse.success(Map.of(
                "reply", reply,
                "session_id", sessionId));
    }

    /**
     * POST /api/v1/chatbot/session/clear
     * Xóa lịch sử hội thoại khi user bắt đầu cuộc trò chuyện mới
     */
    @PostMapping("/session/clear")
    public ApiResponse<Map<String, String>> clearSession(HttpServletRequest httpReq) {
        String token = extractToken(httpReq);
        String userId = jwtService.extractUserId(token);
        String sessionId = "user_" + userId;

        aiChatService.clearSession(sessionId);
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
