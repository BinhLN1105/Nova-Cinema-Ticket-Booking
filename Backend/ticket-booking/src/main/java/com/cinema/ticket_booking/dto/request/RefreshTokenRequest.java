package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
