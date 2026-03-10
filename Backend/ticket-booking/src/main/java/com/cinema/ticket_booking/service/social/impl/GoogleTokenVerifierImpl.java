package com.cinema.ticket_booking.service.social.impl;

import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.service.social.GoogleTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=";

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public SocialUserInfo verify(String accessToken) {
        try {
            // Gọi Google UserInfo API (dùng cho access_token từ flow implicit)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_USER_INFO_URL + accessToken))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("[Google] Token không hợp lệ — HTTP {}", response.statusCode());
                throw new UnauthorizedException("Google token không hợp lệ hoặc đã hết hạn");
            }

            JsonNode payload = objectMapper.readTree(response.body());

            // Kiểm tra lỗi từ Google
            if (payload.has("error_description")) {
                log.warn("[Google] Token lỗi: {}", payload.get("error_description").asText());
                throw new UnauthorizedException("Google token không hợp lệ: "
                        + payload.get("error_description").asText());
            }

            // Kiểm tra email đã xác minh chưa
            boolean emailVerified = "true".equalsIgnoreCase(
                    payload.path("email_verified").asText("false"));

            if (!emailVerified) {
                throw new UnauthorizedException("Email Google chưa được xác minh");
            }

            // Build name
            String fullName = payload.path("name").asText(null);
            if (fullName == null || fullName.isBlank()) {
                String given = payload.path("given_name").asText("");
                String family = payload.path("family_name").asText("");
                fullName = (given + " " + family).trim();
            }

            SocialUserInfo info = SocialUserInfo.builder()
                    .providerId(payload.path("sub").asText()) // Google unique ID
                    .email(payload.path("email").asText())
                    .fullName(fullName)
                    .avatarUrl(payload.path("picture").asText(null))
                    .emailVerified(emailVerified)
                    .build();

            log.info("[Google] Xác minh thành công: providerId={}, email={}",
                    info.getProviderId(), info.getEmail());

            return info;

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Google] Lỗi khi xác minh token", e);
            throw new UnauthorizedException("Không thể xác minh Google token, vui lòng thử lại");
        }
    }
}
