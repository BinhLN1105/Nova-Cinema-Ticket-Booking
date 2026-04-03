package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ChangePasswordRequest;
import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.UserMapper;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.service.UserService;
import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CloudinaryService cloudinaryService;

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
    @Transactional
    public UserResponse updateAvatar(UUID userId, MultipartFile file) throws IOException {
        User user = findById(userId);

        String oldUrl = user.getAvatarUrl();
        String newUrl = null;

        try {
            // 1. Upload New Image to "UserAVT" folder
            newUrl = cloudinaryService.uploadImage(file, "UserAVT");

            // 2. Update Database
            user.setAvatarUrl(newUrl);
            userRepository.save(user);

            // 3. Delete Old Image (If not default)
            if (oldUrl != null && !oldUrl.isEmpty() && !oldUrl.contains("default-avatar")) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null) {
                    cloudinaryService.deleteImageAsync(publicId);
                }
            }

            return userMapper.toResponse(user);

        } catch (Exception e) {
            // Rollback Cloudinary: Nếu lưu DB lỗi, xóa cái ảnh mới vừa up để dọn rác
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null) {
                    cloudinaryService.deleteImageAsync(newPublicId);
                }
            }
            throw new RuntimeException("Cập nhật ảnh đại diện thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse updateAvatarFromUrl(UUID userId, String url) throws IOException {
        User user = findById(userId);
        String oldUrl = user.getAvatarUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImageFromUrl(url, "UserAVT");
            user.setAvatarUrl(newUrl);
            userRepository.save(user);

            if (oldUrl != null && !oldUrl.isEmpty() && !oldUrl.contains("default-avatar")) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null) cloudinaryService.deleteImageAsync(publicId);
            }
            return userMapper.toResponse(user);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null) cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật ảnh đại diện từ URL thất bại: " + e.getMessage());
        }
    }

    @Override
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = findById(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Tài khoản đăng nhập bằng " +
                    user.getAuthProvider().name().toLowerCase() + ", không thể đổi mật khẩu");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 🚨 Bảo mật: Đăng xuất khỏi tất cả thiết bị khi đổi mật khẩu
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }
}
