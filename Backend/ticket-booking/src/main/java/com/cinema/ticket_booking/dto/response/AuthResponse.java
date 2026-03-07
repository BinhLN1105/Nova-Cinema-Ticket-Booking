package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType; // "Bearer"

    // Thông tin user cơ bản để client lưu lại
    private UserResponse user;

    @Data
    @Builder
    public static class UserResponse {
        private String id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private UserRole role;
    }
}
