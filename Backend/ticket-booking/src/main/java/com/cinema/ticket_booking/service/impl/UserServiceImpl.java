package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.UserMapper;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findById(userId);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = findById(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
    }
}
