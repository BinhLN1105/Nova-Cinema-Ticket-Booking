package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.ChangePasswordRequest;
import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.request.NotificationSettingsRequest;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface UserService {

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse updateAvatar(UUID userId, MultipartFile file) throws IOException;

    UserResponse updateAvatarFromUrl(UUID userId, String url) throws IOException;

    void updateFcmToken(UUID userId, String fcmToken);

    void changePassword(UUID userId, ChangePasswordRequest request);

    UserResponse updateNotificationSettings(UUID userId, NotificationSettingsRequest request);

    // Dùng nội bộ trong các Service khác
    User findById(UUID userId);

    User findByEmail(String email);

    void save(User user);
}
