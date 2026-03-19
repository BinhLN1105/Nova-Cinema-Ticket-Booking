package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * AiChatServiceImpl.java
 * HTTP client để Java gọi sang Python RAG server.
 * Dùng Spring WebClient (reactive, non-blocking).
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Value("${nova.python.rag.url}")
    private String pythonRagUrl;

    @Value("${nova.internal.api-key}")
    private String internalApiKey;

    private final WebClient webClient;

    public AiChatServiceImpl() {
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Override
    public String sendMessage(String sessionId, String userMessage) {
        try {
            var body = Map.of("session_id", sessionId, "user_message", userMessage);

            var response = webClient.post()
                    .uri(pythonRagUrl + "/api/v1/chat")
                    .header("Content-Type", "application/json")
                    // Không cần gửi internal key cho /chat — chỉ cần cho /sync
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30)) // AI có thể chậm, 30s timeout
                    .block();

            if (response != null && response.containsKey("reply")) {
                return (String) response.get("reply");
            }
            return fallbackMessage();

        } catch (WebClientResponseException e) {
            log.error("[AiChat] Python server error: {} {}", e.getStatusCode(), e.getMessage());
            return fallbackMessage();
        } catch (Exception e) {
            log.error("[AiChat] Connection error: {}", e.getMessage());
            return fallbackMessage();
        }
    }

    @Override
    public void clearSession(String sessionId) {
        try {
            webClient.post()
                    .uri(pythonRagUrl + "/api/v1/session/clear?session_id=" + sessionId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.warn("[AiChat] Could not clear session: {}", e.getMessage());
        }
    }

    @Override
    public boolean triggerSync() {
        try {
            var response = webClient.post()
                    .uri(pythonRagUrl + "/api/v1/sync")
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMinutes(5)) // Ingest có thể mất vài phút
                    .block();

            return response != null && "success".equals(response.get("status"));
        } catch (Exception e) {
            log.error("[AiChat] Sync trigger failed: {}", e.getMessage());
            return false;
        }
    }

    private String fallbackMessage() {
        return "Xin lỗi anh/chị, em đang gặp sự cố kỹ thuật. " +
                "Anh/chị vui lòng thử lại sau hoặc liên hệ hotline 1900 6789 để được hỗ trợ ạ.";
    }
}
