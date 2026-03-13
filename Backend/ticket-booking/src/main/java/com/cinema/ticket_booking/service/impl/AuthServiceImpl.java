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
import com.cinema.ticket_booking.service.social.FacebookTokenVerifier;
import com.cinema.ticket_booking.service.social.GoogleTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
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
    private final GoogleTokenVerifier googleTokenVerifier;
    private final FacebookTokenVerifier facebookTokenVerifier;

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

    // ── Social Login ──────────────────────────────────────────────────────

    @Override
    public AuthResponse socialLogin(SocialLoginRequest request) {
        // Bước 1: Uỷ quyền xác minh token cho đúng verifier
        SocialUserInfo info = switch (request.getProvider()) {
            case GOOGLE -> googleTokenVerifier.verify(request.getIdToken());
            case FACEBOOK -> facebookTokenVerifier.verify(request.getIdToken());
            default -> throw new BadRequestException(
                    "Provider không được hỗ trợ: " + request.getProvider());
        };

        // Bước 2: Tìm hoặc tạo user từ thông tin Social
        User user = findOrCreateSocialUser(info, request.getProvider());

        validateActiveUser(user);

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

        // Chỉ cấp lại access token mới, giữ nguyên refresh token cũ
        String newAccessToken = jwtService.generateAccessToken(stored.getUser());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .user(toUserInfo(stored.getUser()))
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Override
    public void logout(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);
        if (stored != null) {
            refreshTokenRepository.deleteAllByUser(stored.getUser());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Tìm user theo providerId.
     * Nếu chưa tồn tại → kiểm tra email → tạo mới hoặc liên kết account LOCAL.
     */
    private User findOrCreateSocialUser(SocialUserInfo info, AuthProvider provider) {
        // Trường hợp 1: Đã từng đăng nhập Social trước đó → tìm theo providerId
        return userRepository.findByAuthProviderAndProviderId(provider, info.getProviderId())
                .orElseGet(() -> {
                    // Trường hợp 2: Email đã có tài khoản LOCAL → liên kết Social vào
                    if (userRepository.existsByEmail(info.getEmail())) {
                        User existing = userRepository.findByEmail(info.getEmail()).get();
                        existing.setAuthProvider(provider);
                        existing.setProviderId(info.getProviderId());
                        if (existing.getAvatarUrl() == null) {
                            existing.setAvatarUrl(info.getAvatarUrl());
                        }
                        return userRepository.save(existing);
                    }

                    // Trường hợp 3: Người dùng hoàn toàn mới → tạo tài khoản
                    User newUser = User.builder()
                            .email(info.getEmail())
                            .fullName(info.getFullName())
                            .avatarUrl(info.getAvatarUrl())
                            .authProvider(provider)
                            .providerId(info.getProviderId())
                            .role(UserRole.CUSTOMER)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /** Tạo access token + refresh token, lưu refresh token vào DB */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        // Xoá refresh token cũ → lưu token mới (1 thiết bị = 1 refresh token)
        // Nếu muốn hỗ trợ multi-device: bỏ dòng deleteAllByUser
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

    private void validateActiveUser(User user) {
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị khoá, vui lòng liên hệ hỗ trợ");
        }
    }

    private AuthResponse.UserResponse toUserInfo(User user) {
        return AuthResponse.UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .rewardPoints(user.getRewardPoints())
                .availableExp(user.getAvailableExp())
                .membershipTier(user.getMembershipTier())
                .build();
    }
}
