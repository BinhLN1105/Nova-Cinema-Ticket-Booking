package com.cinema.ticket_booking.service.social.impl;

import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.service.social.GoogleTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final WebClient webClient;

    @Override
    public SocialUserInfo verify(String tokenString) {
        if (tokenString == null || tokenString.trim().isEmpty()) {
            log.warn("[Google] Token null hoặc trống");
            throw new UnauthorizedException("Google token không được để trống");
        }

        // Check if token is a possible JWT (has two dots)
        boolean isJwt = tokenString.chars().filter(c -> c == '.').count() == 2;

        if (isJwt) {
            return verifyIdToken(tokenString);
        } else {
            return verifyAccessToken(tokenString);
        }
    }

    private SocialUserInfo verifyIdToken(String idTokenString) {
        try {
            // Verify ID Token locally (cryptographic signature + audience)
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("[Google] Token ID không hợp lệ, signature mismatch hoặc audience không khớp");
                throw new UnauthorizedException("Google token không hợp lệ hoặc đã hết hạn");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verification success, map fields to our DTO
            SocialUserInfo info = SocialUserInfo.builder()
                    .providerId(payload.getSubject()) // 'sub'
                    .email(payload.getEmail())
                    .fullName((String) payload.get("name"))
                    .avatarUrl((String) payload.get("picture"))
                    .emailVerified(payload.getEmailVerified())
                    .build();

            log.info("[Google] Xác minh thành công (Local JWT Cache): email={}", info.getEmail());

            return info;

        } catch (UnauthorizedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("[Google] Định dạng token không hợp lệ (Không phải JWT). Received: {}", idTokenString);
            throw new UnauthorizedException("Định dạng Token Google không hợp lệ (Yêu cầu JWT)");
        } catch (Exception e) {
            log.error("[Google] Lỗi hệ thống khi xác minh token ID", e);
            throw new UnauthorizedException("Không thể xác minh Google token, vui lòng thử lại sau");
        }
    }

    private SocialUserInfo verifyAccessToken(String accessToken) {
        try {
            log.info("[Google] Đang xác minh Access Token qua Google API");

            // Call Google UserInfo API
            Map<String, Object> userInfo = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (userInfo == null || !userInfo.containsKey("email")) {
                log.warn("[Google] Không lấy được thông tin email từ Access Token");
                throw new UnauthorizedException("Google token không hợp lệ hoặc đã hết hạn");
            }

            SocialUserInfo info = SocialUserInfo.builder()
                    .providerId((String) userInfo.get("sub"))
                    .email((String) userInfo.get("email"))
                    .fullName((String) userInfo.get("name"))
                    .avatarUrl((String) userInfo.get("picture"))
                    .emailVerified(Boolean.TRUE.equals(userInfo.get("email_verified")))
                    .build();

            log.info("[Google] Xác minh thành công (UserInfo API): email={}", info.getEmail());

            return info;

        } catch (Exception e) {
            log.error("[Google] Lỗi hệ thống khi xác minh Access Token", e);
            throw new UnauthorizedException("Không thể xác minh Google access token, vui lòng thử lại sau");
        }
    }
}
