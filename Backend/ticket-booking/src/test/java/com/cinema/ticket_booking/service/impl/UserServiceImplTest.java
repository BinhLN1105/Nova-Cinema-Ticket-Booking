package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ChangePasswordRequest;
import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.UserMapper;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private StaffProfileRepository staffProfileRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@gmail.com")
                .fullName("Nguyễn Văn A")
                .password("encoded_pass")
                .role(UserRole.CUSTOMER)
                .membershipTier(MembershipTier.BRONZE)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        userResponse = UserResponse.builder()
                .id(userId.toString())
                .email("user@gmail.com")
                .fullName("Nguyễn Văn A")
                .role(UserRole.CUSTOMER)
                .membershipTier(MembershipTier.BRONZE)
                .build();
    }

    @Test
    @DisplayName("1. getProfile thành công khi user tồn tại")
    void testGetProfileSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getProfile(userId);
        assertNotNull(result);
        assertEquals(userId.toString(), result.getId());
        assertEquals("user@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("2. getProfile ném ResourceNotFoundException khi không tìm thấy user")
    void testGetProfileNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(userId));
    }

    @Test
    @DisplayName("3. updateProfile thành công")
    void testUpdateProfileSuccess() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyễn Văn B");
        request.setPhone("0987654321");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateProfile(userId, request);
        assertNotNull(result);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("4. changePassword thất bại khi mật khẩu cũ không đúng")
    void testChangePasswordWrongOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong_old");
        request.setNewPassword("new_pass123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_old", "encoded_pass")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changePassword(userId, request));
    }

    @Test
    @DisplayName("5. changePassword thành công khi mật khẩu cũ chính xác")
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("correct_old");
        request.setNewPassword("new_pass123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct_old", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.encode("new_pass123")).thenReturn("new_encoded_pass");

        userService.changePassword(userId, request);
        verify(userRepository, times(1)).save(user);
        verify(refreshTokenRepository, times(1)).deleteAllByUser(user);
    }
}
