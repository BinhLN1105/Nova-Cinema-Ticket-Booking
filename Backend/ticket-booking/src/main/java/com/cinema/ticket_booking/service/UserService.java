package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;

import java.util.UUID;

public interface UserService {

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void updateFcmToken(UUID userId, String fcmToken);

    // Dùng nội bộ trong các Service khác
    User findById(UUID userId);
}
