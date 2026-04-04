package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.ChangePasswordRequest;
import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.request.FcmTokenRequest;
import com.cinema.ticket_booking.dto.request.NotificationSettingsRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.UserService;
import com.cinema.ticket_booking.service.UserVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserVoucherService userVoucherService;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getProfile(currentUser.getId())));
    }

    // PATCH /api/v1/users/me
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(currentUser.getId(), request), "Cập nhật thành công"));
    }

    // PATCH /api/v1/users/me/fcm-token
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody FcmTokenRequest request) {
        userService.updateFcmToken(currentUser.getId(), request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật FCM token thành công"));
    }

    // PATCH /api/v1/users/me/notifications
    @PatchMapping("/me/notifications")
    public ResponseEntity<ApiResponse<UserResponse>> updateNotificationSettings(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateNotificationSettings(currentUser.getId(), request), 
                "Cập nhật cài đặt thông báo thành công"));
    }

    // POST /api/v1/users/me/avatar
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // 1. Validate File Size (<10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Dung lượng ảnh vượt quá 10MB. Vui lòng nén ảnh lại.");
        }

        // 2. Validate format
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ (cần file ảnh jpeg, png, webp)");
        }

        // 3. Upload & update via Service (Safe-fail logic)
        UserResponse updatedUser = userService.updateAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Cập nhật ảnh đại diện thành công"));
    }

    @PostMapping("/me/avatar-url")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatarViaUrl(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> request) throws IOException {
        String url = request.get("url");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Vui lòng cung cấp URL ảnh");
        
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateAvatarFromUrl(currentUser.getId(), url), 
                "Cập nhật ảnh đại diện từ URL thành công"));
    }

    // PUT /api/v1/users/me/password
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    // POST /api/v1/users/me/vouchers/{voucherId}
    @PostMapping("/me/vouchers/{voucherId}")
    public ResponseEntity<ApiResponse<Void>> saveVoucher(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID voucherId) {
        userVoucherService.saveVoucher(currentUser.getId(), voucherId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã lưu mã ưu đãi vào ví"));
    }

    // GET /api/v1/users/me/vouchers
    @GetMapping("/me/vouchers")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyVouchers(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                userVoucherService.getMyVouchers(currentUser.getId())));
    }
}
