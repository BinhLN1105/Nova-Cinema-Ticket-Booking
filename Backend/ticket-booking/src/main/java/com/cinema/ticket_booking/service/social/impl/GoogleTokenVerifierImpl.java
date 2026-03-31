package com.cinema.ticket_booking.service.social.impl;

import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.service.social.GoogleTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Override
    public SocialUserInfo verify(String idTokenString) {
        try {
            // Verify ID Token locally (cryptographic signature + audience)
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("[Google] Token ID không hợp lệ hoặc đã hết hạn");
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
        } catch (Exception e) {
            log.error("[Google] Lỗi hệ thống khi xác minh token", e);
            throw new UnauthorizedException("Không thể xác minh Google token, vui lòng thử lại sau");
        }
    }
}
