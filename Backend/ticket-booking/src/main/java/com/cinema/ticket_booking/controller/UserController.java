package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.ChangePasswordRequest;
import com.cinema.ticket_booking.dto.request.UpdateProfileRequest;
import com.cinema.ticket_booking.dto.request.FcmTokenRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.UserService;
import com.cinema.ticket_booking.service.UserVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // PATCH /api/v1/users/me/password
    @PatchMapping("/me/password")
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
