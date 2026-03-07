package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SocialLoginRequest {

    // ID token từ Google / Facebook SDK trên Android
    @NotBlank(message = "ID token không được để trống")
    private String idToken;

    @NotNull(message = "Provider không được để trống")
    private AuthProvider provider; // GOOGLE | FACEBOOK
}
