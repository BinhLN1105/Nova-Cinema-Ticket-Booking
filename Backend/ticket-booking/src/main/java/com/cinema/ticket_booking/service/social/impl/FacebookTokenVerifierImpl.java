package com.cinema.ticket_booking.service.social.impl;

import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.service.social.FacebookTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class FacebookTokenVerifierImpl implements FacebookTokenVerifier {

    private static final String FB_DEBUG_URL = "https://graph.facebook.com/debug_token";
    private static final String FB_ME_URL = "https://graph.facebook.com/me";

    private final ObjectMapper objectMapper;

    @Value("${app.social.facebook.client-id}")
    private String appId;

    @Value("${app.social.facebook.client-secret}")
    private String appSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public SocialUserInfo verify(String accessToken) {
        try {
            // ── Bước 1: Debug token để xác minh tính hợp lệ ──────────────
            debugToken(accessToken);

            // ── Bước 2: Lấy thông tin user từ Graph API ───────────────────
            return fetchUserInfo(accessToken);

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Facebook] Lỗi khi xác minh token", e);
            throw new UnauthorizedException("Không thể xác minh Facebook token, vui lòng thử lại");
        }
    }

    // ── Private ───────────────────────────────────────────────────────────

    /**
     * Bước 1: Gọi debug_token API để xác minh token hợp lệ và thuộc đúng app.
     */
    private void debugToken(String accessToken) throws Exception {
        String appToken = appId + "|" + appSecret;
        String url = FB_DEBUG_URL
                + "?input_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&access_token=" + URLEncoder.encode(appToken, StandardCharsets.UTF_8);

        JsonNode data = callGet(url).path("data");

        // Kiểm tra is_valid
        boolean isValid = data.path("is_valid").asBoolean(false);
        if (!isValid) {
            String error = data.path("error").path("message").asText("Token không hợp lệ");
            log.warn("[Facebook] Debug token thất bại: {}", error);
            throw new UnauthorizedException("Facebook token không hợp lệ: " + error);
        }

        // Xác minh token thuộc đúng app
        String tokenAppId = data.path("app_id").asText("");
        if (!appId.equals(tokenAppId)) {
            log.warn("[Facebook] App ID không khớp — expected={}, got={}", appId, tokenAppId);
            throw new UnauthorizedException("Facebook token không thuộc về ứng dụng này");
        }

        // Kiểm tra hết hạn
        long expiresAt = data.path("expires_at").asLong(0);
        if (expiresAt > 0 && expiresAt < System.currentTimeMillis() / 1000) {
            throw new UnauthorizedException("Facebook token đã hết hạn");
        }

        log.debug("[Facebook] Debug token hợp lệ — userId={}", data.path("user_id").asText());
    }

    /**
     * Bước 2: Lấy id, name, email, picture từ Graph API /me.
     */
    private SocialUserInfo fetchUserInfo(String accessToken) throws Exception {
        String url = FB_ME_URL
                + "?fields=id,name,email,picture.type(large)"
                + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        JsonNode me = callGet(url);

        // Facebook không bắt buộc user cấp quyền email — hoặc user đăng ký bằng SĐT
        String email = me.path("email").asText(null);
        String providerId = me.path("id").asText();

        if (email == null || email.isBlank()) {
            log.warn("[Facebook] User chưa cấp quyền email (hoặc không có) — providerId={}. Tạo email mặc định.", providerId);
            email = providerId + "@facebook.com";
        }

        String avatarUrl = me.path("picture").path("data").path("url").asText(null);

        SocialUserInfo info = SocialUserInfo.builder()
                .providerId(providerId)
                .email(email)
                .fullName(me.path("name").asText(""))
                .avatarUrl(avatarUrl)
                .emailVerified(false) // Facebook không xác minh email như Google
                .build();

        log.info("[Facebook] Xác minh thành công: providerId={}, email={}",
                info.getProviderId(), info.getEmail());

        return info;
    }

    /** Gọi GET request và parse JSON response */
    private JsonNode callGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        JsonNode body = objectMapper.readTree(response.body());

        // Kiểm tra lỗi từ Facebook Graph API
        if (body.has("error")) {
            String msg = body.path("error").path("message").asText("Lỗi Facebook API");
            log.warn("[Facebook] Graph API lỗi: {}", msg);
            throw new UnauthorizedException("Facebook API lỗi: " + msg);
        }

        return body;
    }
}
