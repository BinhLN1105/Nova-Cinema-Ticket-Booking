package com.cinema.ticket_booking.service.social;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin user trả về sau khi xác minh token Social (Google / Facebook).
 * Chuẩn hoá thành 1 object dùng chung để AuthServiceImpl không cần
 * biết token đến từ provider nào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {

    /** ID duy nhất từ provider (Google sub / Facebook id) */
    private String providerId;

    private String email;
    private String fullName;
    private String avatarUrl;

    /** Đã xác minh email chưa (Google luôn true, Facebook cần kiểm tra) */
    private boolean emailVerified;
}
