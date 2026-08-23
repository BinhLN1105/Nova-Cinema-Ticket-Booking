package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSearchResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private MembershipTier membershipTier;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
