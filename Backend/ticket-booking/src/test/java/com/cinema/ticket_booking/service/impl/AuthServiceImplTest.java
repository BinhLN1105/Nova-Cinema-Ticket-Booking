package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.RefreshTokenRequest;
import com.cinema.ticket_booking.dto.request.SocialLoginRequest;
import com.cinema.ticket_booking.dto.response.AuthResponse;
import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.UnauthorizedException;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.RefreshToken;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.EmailService;
import com.cinema.ticket_booking.service.JwtService;
import com.cinema.ticket_booking.service.social.FacebookTokenVerifier;
import com.cinema.ticket_booking.service.social.GoogleTokenVerifier;
import com.cinema.ticket_booking.service.social.SocialUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private FacebookTokenVerifier facebookTokenVerifier;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private Environment env;

    @InjectMocks
    private AuthServiceImpl authService;

    private User customerUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiryDays", 30);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        customerUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .fullName("Customer Test")
                .password("encoded_pwd")
                .role(UserRole.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        staffUser = User.builder()
                .id(UUID.randomUUID())
                .email("staff@test.com")
                .fullName("Staff Test")
                .password("encoded_pwd")
                .role(UserRole.STAFF)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("1. socialLogin Google - tạo mới người dùng khi chưa có tài khoản")
    void testSocialLogin_Google_NewUser() {
        SocialLoginRequest request = new SocialLoginRequest();
        request.setProvider(AuthProvider.GOOGLE);
        request.setIdToken("google_token");

        SocialUserInfo userInfo = SocialUserInfo.builder()
                .providerId("google_id_123")
                .email("newuser@gmail.com")
                .fullName("New User")
                .avatarUrl("avatar.jpg")
                .emailVerified(true)
                .build();
        when(googleTokenVerifier.verify("google_token")).thenReturn(userInfo);
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google_id_123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtService.generateAccessToken(any())).thenReturn("mock_acc_token");
        when(jwtService.generateRefreshToken()).thenReturn("mock_ref_token");

        AuthResponse response = authService.socialLogin(request);
        assertNotNull(response);
        assertEquals("mock_acc_token", response.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("2. socialLogin Google - liên kết tài khoản khi email đã tồn tại ở local")
    void testSocialLogin_Google_LinkExistingUser() {
        SocialLoginRequest request = new SocialLoginRequest();
        request.setProvider(AuthProvider.GOOGLE);
        request.setIdToken("google_token");

        SocialUserInfo userInfo = SocialUserInfo.builder()
                .providerId("google_id_123")
                .email("customer@test.com")
                .fullName("Customer Test")
                .avatarUrl(null)
                .emailVerified(true)
                .build();
        when(googleTokenVerifier.verify("google_token")).thenReturn(userInfo);
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google_id_123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("customer@test.com")).thenReturn(true);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.save(customerUser)).thenReturn(customerUser);

        when(jwtService.generateAccessToken(any())).thenReturn("mock_acc_token");
        when(jwtService.generateRefreshToken()).thenReturn("mock_ref_token");

        AuthResponse response = authService.socialLogin(request);
        assertNotNull(response);
        assertEquals(AuthProvider.GOOGLE, customerUser.getAuthProvider());
        assertEquals("google_id_123", customerUser.getProviderId());
    }

    @Test
    @DisplayName("3. socialLogin Facebook - người dùng đã từng đăng nhập trước đó")
    void testSocialLogin_Facebook_ExistingSocialUser() {
        SocialLoginRequest request = new SocialLoginRequest();
        request.setProvider(AuthProvider.FACEBOOK);
        request.setIdToken("fb_token");

        SocialUserInfo userInfo = SocialUserInfo.builder()
                .providerId("fb_id_456")
                .email("fbuser@test.com")
                .fullName("FB User")
                .avatarUrl("fb_avatar.jpg")
                .emailVerified(true)
                .build();
        when(facebookTokenVerifier.verify("fb_token")).thenReturn(userInfo);
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.FACEBOOK, "fb_id_456")).thenReturn(Optional.of(customerUser));

        when(jwtService.generateAccessToken(any())).thenReturn("mock_acc_token");
        when(jwtService.generateRefreshToken()).thenReturn("mock_ref_token");

        AuthResponse response = authService.socialLogin(request);
        assertNotNull(response);
    }

    @Test
    @DisplayName("4. socialLogin ném lỗi khi tài khoản đã bị khóa")
    void testSocialLogin_InactiveUser_ThrowsException() {
        SocialLoginRequest request = new SocialLoginRequest();
        request.setProvider(AuthProvider.GOOGLE);
        request.setIdToken("google_token");

        customerUser.setIsActive(false);
        SocialUserInfo userInfo = SocialUserInfo.builder()
                .providerId("google_id_123")
                .email("customer@test.com")
                .fullName("Customer Test")
                .avatarUrl(null)
                .emailVerified(true)
                .build();
        when(googleTokenVerifier.verify("google_token")).thenReturn(userInfo);
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google_id_123")).thenReturn(Optional.of(customerUser));

        assertThrows(BadRequestException.class, () -> authService.socialLogin(request));
    }

    @Test
    @DisplayName("5. refreshToken thành công khi token còn hạn")
    void testRefreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .token("valid_refresh_token")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();

        when(refreshTokenRepository.findByToken("valid_refresh_token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(customerUser)).thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(request);
        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("valid_refresh_token", response.getRefreshToken());
    }

    @Test
    @DisplayName("6. refreshToken ném UnauthorizedException khi token hết hạn")
    void testRefreshToken_Expired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired_token");

        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .token("expired_token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(stored));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository, times(1)).delete(stored);
    }

    @Test
    @DisplayName("7. logout xóa hết refresh token của user")
    void testLogout_Success() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .token("my_refresh_token")
                .build();
        when(refreshTokenRepository.findByToken("my_refresh_token")).thenReturn(Optional.of(stored));

        authService.logout("my_refresh_token");
        verify(refreshTokenRepository, times(1)).deleteAllByUser(customerUser);
    }

    @Test
    @DisplayName("8. requestPasswordReset chặn ADMIN/STAFF")
    void testRequestPasswordReset_BlockStaff() {
        when(userRepository.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));

        authService.requestPasswordReset("staff@test.com");
        verify(emailService, never()).sendPasswordResetOtpEmail(any(), any());
    }

    @Test
    @DisplayName("9. requestPasswordReset gửi email cho CUSTOMER khi ở profile test")
    void testRequestPasswordReset_Customer_TestProfile() {
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customerUser));
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});

        authService.requestPasswordReset("customer@test.com");

        verify(valueOperations, times(1)).set(eq("otp:customer@test.com"), eq("123456"), eq(5L), eq(TimeUnit.MINUTES));
        verify(emailService, times(1)).sendPasswordResetOtpEmail(eq(customerUser), eq("123456"));
    }

    @Test
    @DisplayName("10. verifyOtp ném lỗi khi vượt quá 5 lần brute force")
    void testVerifyOtp_BruteForceLimit() {
        when(valueOperations.get("otp_attempts:customer@test.com")).thenReturn("5");

        assertThrows(BadRequestException.class, () -> authService.verifyOtp("customer@test.com", "123456"));
    }

    @Test
    @DisplayName("11. verifyOtp ném lỗi khi OTP hết hạn (null)")
    void testVerifyOtp_Expired() {
        when(valueOperations.get("otp_attempts:customer@test.com")).thenReturn("0");
        when(valueOperations.get("otp:customer@test.com")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> authService.verifyOtp("customer@test.com", "123456"));
    }

    @Test
    @DisplayName("12. verifyOtp ném lỗi khi OTP sai và tăng attempt counter")
    void testVerifyOtp_WrongOtp() {
        when(valueOperations.get("otp_attempts:customer@test.com")).thenReturn("0");
        when(valueOperations.get("otp:customer@test.com")).thenReturn("999999");

        assertThrows(BadRequestException.class, () -> authService.verifyOtp("customer@test.com", "123456"));
        verify(valueOperations, times(1)).increment("otp_attempts:customer@test.com");
    }

    @Test
    @DisplayName("13. verifyOtp thành công trả về reset token")
    void testVerifyOtp_Success() {
        when(valueOperations.get("otp_attempts:customer@test.com")).thenReturn("0");
        when(valueOperations.get("otp:customer@test.com")).thenReturn("123456");

        String resetToken = authService.verifyOtp("customer@test.com", "123456");
        assertNotNull(resetToken);
        verify(valueOperations, times(1)).set(startsWith("reset_token:"), eq("customer@test.com"), eq(10L), eq(TimeUnit.MINUTES));
        verify(redisTemplate, times(1)).delete("otp:customer@test.com");
    }

    @Test
    @DisplayName("14. resetPassword thành công và cập nhật mật khẩu mã hóa mới")
    void testResetPassword_Success() {
        when(valueOperations.get("reset_token:valid_token")).thenReturn("customer@test.com");
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.encode("new_secure_pwd")).thenReturn("encoded_new_pwd");

        authService.resetPassword("valid_token", "new_secure_pwd");

        assertEquals("encoded_new_pwd", customerUser.getPassword());
        verify(userRepository, times(1)).save(customerUser);
        verify(redisTemplate, times(1)).delete("reset_token:valid_token");
        verify(refreshTokenRepository, times(1)).deleteAllByUser(customerUser);
    }

    @Test
    @DisplayName("15. resetPassword ném lỗi khi token không tồn tại trong redis")
    void testResetPassword_InvalidToken() {
        when(valueOperations.get("reset_token:invalid_token")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> authService.resetPassword("invalid_token", "pwd"));
    }

    @Test
    @DisplayName("16. toUserInfo hiển thị cinemaId khi role là STAFF và có gắn Cinema")
    void testToUserInfo_StaffWithCinema() {
        UUID cinemaId = UUID.randomUUID();
        Cinema mockCinema = Cinema.builder().id(cinemaId).name("Nova Hà Nội").build();
        StaffProfile profile = StaffProfile.builder().cinema(mockCinema).build();

        when(staffProfileRepository.findByUserId(staffUser.getId())).thenReturn(Optional.of(profile));
        when(jwtService.generateAccessToken(staffUser)).thenReturn("staff_acc_token");

        // Gọi buildAuthResponse gián tiếp qua refreshToken
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(staffUser)
                .token("staff_refresh_token")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();
        when(refreshTokenRepository.findByToken("staff_refresh_token")).thenReturn(Optional.of(stored));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("staff_refresh_token");

        AuthResponse resp = authService.refreshToken(req);
        assertNotNull(resp.getUser());
        assertEquals(cinemaId.toString(), resp.getUser().getCinemaId());
    }
}
