package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.LoginRequest;
import com.cinema.ticket_booking.dto.request.RefreshTokenRequest;
import com.cinema.ticket_booking.dto.request.RegisterRequest;
import com.cinema.ticket_booking.dto.request.SocialLoginRequest;
import com.cinema.ticket_booking.dto.response.AuthResponse;
import com.cinema.ticket_booking.model.RefreshToken;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.AuthService;
import com.cinema.ticket_booking.service.JwtService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiry-days:30}")
    private int refreshExpiryDays;

    // ── Register ──────────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email " + request.getEmail() + " đã được đăng ký");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(UserRole.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    // ── Login LOCAL ───────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng"));

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị khoá");
        }
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Tài khoản này đăng nhập bằng " +
                    user.getAuthProvider().name().toLowerCase() + ", vui lòng dùng đúng phương thức");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(user);
    }

    // ── Social Login (Google / Facebook) ─────────────────────────────────

    @Override
    public AuthResponse socialLogin(SocialLoginRequest request) {
        SocialUserInfo info = verifySocialToken(request.getIdToken(), request.getProvider());

        User user = userRepository
                .findByAuthProviderAndProviderId(request.getProvider(), info.getProviderId())
                .orElseGet(() -> {
                    // Email đã tồn tại dạng LOCAL → liên kết account
                    if (userRepository.existsByEmail(info.getEmail())) {
                        User existing = userRepository.findByEmail(info.getEmail()).get();
                        existing.setAuthProvider(request.getProvider());
                        existing.setProviderId(info.getProviderId());
                        if (existing.getAvatarUrl() == null)
                            existing.setAvatarUrl(info.getAvatarUrl());
                        return userRepository.save(existing);
                    }
                    // Tạo tài khoản mới từ Social
                    return userRepository.save(User.builder()
                            .email(info.getEmail())
                            .fullName(info.getFullName())
                            .avatarUrl(info.getAvatarUrl())
                            .authProvider(request.getProvider())
                            .providerId(info.getProviderId())
                            .role(UserRole.CUSTOMER)
                            .isActive(true)
                            .build());
                });

        if (!user.getIsActive())
            throw new BadRequestException("Tài khoản đã bị khoá");
        return buildAuthResponse(user);
    }

    // ── Refresh Token ─────────────────────────────────────────────────────

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }

        String newAccess = jwtService.generateAccessToken(stored.getUser());
        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .user(toUserInfo(stored.getUser()))
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Override
    public void logout(User currentUser) {
        refreshTokenRepository.deleteAllByUser(currentUser);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        refreshTokenRepository.deleteAllByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpiryDays))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toUserInfo(user))
                .build();
    }

    private AuthResponse.UserResponse toUserInfo(User u) {
        return AuthResponse.UserResponse.builder()
                .id(u.getId().toString())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .build();
    }

    /**
     * Xác minh idToken với Google / Facebook.
     * TODO: tích hợp Google Auth Library hoặc Facebook Graph API.
     */
    private SocialUserInfo verifySocialToken(String idToken, AuthProvider provider) {
        throw new UnsupportedOperationException("Implement social token verification");
    }

    @Data
    @AllArgsConstructor
    public static class SocialUserInfo {
        private String providerId;
        private String email;
        private String fullName;
        private String avatarUrl;
    }
}
