package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.UserRole;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserRole role;
    private AuthProvider authProvider;
    private Boolean isActive;
    private Long rewardPoints;
    private Long availableExp;
    private MembershipTier membershipTier;
    private Long currentTierMinPoints;
    private Long nextTierMinPoints;
    private LocalDateTime createdAt;
    /** Chỉ có giá trị khi role == STAFF, null với CUSTOMER/ADMIN */
    private String cinemaId;
    private String cinemaName;
}
