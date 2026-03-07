package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserRole role;
    private AuthProvider authProvider;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
