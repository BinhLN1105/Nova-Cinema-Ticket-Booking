package com.cinema.ticket_booking.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        aiChatService = new AiChatServiceImpl(redisTemplate);

        ReflectionTestUtils.setField(aiChatService, "pythonRagUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(aiChatService, "internalApiKey", "mock-secret-key");
        ReflectionTestUtils.setField(aiChatService, "webClient", webClient);
    }

    private void setupWebClientPostMock() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("1. sendMessage thành công khi quota còn hạn mức (< 500)")
    void testSendMessage_SuccessWithinQuota() {
        setupWebClientPostMock();

        when(valueOperations.get(startsWith("ai_quota:global:"))).thenReturn("10");
        when(valueOperations.increment(startsWith("ai_quota:global:"))).thenReturn(11L);

        Map<String, Object> mockResponse = Map.of("reply", "Chào bạn, hôm nay có phim Đào Phở và Piano nhé!");
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        Map<String, Object> result = aiChatService.sendMessage("sess-1", "Hôm nay có phim gì?");

        assertNotNull(result);
        assertEquals("Chào bạn, hôm nay có phim Đào Phở và Piano nhé!", result.get("reply"));
        assertEquals(false, result.get("used_fallback"));
    }

    @Test
    @DisplayName("2. sendMessage thiết lập TTL khi lượt gọi đầu tiên trong ngày (newCount == 1)")
    void testSendMessage_FirstCallOfDaySetsExpire() {
        setupWebClientPostMock();

        when(valueOperations.get(startsWith("ai_quota:global:"))).thenReturn(null);
        when(valueOperations.increment(startsWith("ai_quota:global:"))).thenReturn(1L);

        Map<String, Object> mockResponse = Map.of("reply", "Chào bạn!", "used_fallback", false);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        Map<String, Object> result = aiChatService.sendMessage("sess-1", "Alo");

        assertNotNull(result);
        verify(redisTemplate, times(1)).expire(startsWith("ai_quota:global:"), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("3. sendMessage vượt quá quota (>= 500) tự động bật cờ fallback")
    void testSendMessage_QuotaExceeded() {
        setupWebClientPostMock();

        when(valueOperations.get(startsWith("ai_quota:global:"))).thenReturn("500");

        Map<String, Object> mockResponse = Map.of("reply", "Dạ chào anh/chị, tôi là trợ lý ảo.");
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        Map<String, Object> result = aiChatService.sendMessage("sess-1", "Đặt vé");

        assertNotNull(result);
        assertEquals(true, result.get("used_fallback"));
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("4. sendMessage xử lý WebClientResponseException và trả về fallback message")
    void testSendMessage_WebClientResponseException() {
        setupWebClientPostMock();

        when(valueOperations.get(startsWith("ai_quota:global:"))).thenReturn("0");
        when(valueOperations.increment(startsWith("ai_quota:global:"))).thenReturn(1L);

        WebClientResponseException ex = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(ex));

        Map<String, Object> result = aiChatService.sendMessage("sess-1", "Test error");

        assertNotNull(result);
        assertTrue(result.get("reply").toString().contains("sự cố kỹ thuật"));
        assertEquals(true, result.get("used_fallback"));
    }

    @Test
    @DisplayName("5. sendMessage xử lý ngoại lệ chung (timeout/mạng) và trả về fallback message")
    void testSendMessage_GeneralException() {
        setupWebClientPostMock();

        when(valueOperations.get(startsWith("ai_quota:global:"))).thenReturn("0");
        when(valueOperations.increment(startsWith("ai_quota:global:"))).thenReturn(1L);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Map<String, Object> result = aiChatService.sendMessage("sess-1", "Test connection error");

        assertNotNull(result);
        assertTrue(result.get("reply").toString().contains("sự cố kỹ thuật"));
        assertEquals(true, result.get("used_fallback"));
    }

    @Test
    @DisplayName("6. clearSession thành công")
    void testClearSession_Success() {
        setupWebClientPostMock();
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> aiChatService.clearSession("sess-123"));
    }

    @Test
    @DisplayName("7. clearSession nuốt ngoại lệ an toàn nếu server lỗi")
    void testClearSession_ExceptionHandled() {
        setupWebClientPostMock();
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new RuntimeException("Server error")));

        assertDoesNotThrow(() -> aiChatService.clearSession("sess-123"));
    }

    @Test
    @DisplayName("8. triggerSync trả về true khi response status = success")
    void testTriggerSync_Success() {
        setupWebClientPostMock();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("status", "success")));

        boolean result = aiChatService.triggerSync();
        assertTrue(result);
    }

    @Test
    @DisplayName("9. triggerSync trả về false khi response status != success")
    void testTriggerSync_FailureStatus() {
        setupWebClientPostMock();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("status", "failed")));

        boolean result = aiChatService.triggerSync();
        assertFalse(result);
    }

    @Test
    @DisplayName("10. triggerSync trả về false khi gặp ngoại lệ")
    void testTriggerSync_Exception() {
        setupWebClientPostMock();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("Network down")));

        boolean result = aiChatService.triggerSync();
        assertFalse(result);
    }
}
