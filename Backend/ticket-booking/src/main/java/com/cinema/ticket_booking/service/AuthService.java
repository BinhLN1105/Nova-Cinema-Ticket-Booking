package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.LoginRequest;
import com.cinema.ticket_booking.dto.request.RefreshTokenRequest;
import com.cinema.ticket_booking.dto.request.RegisterRequest;
import com.cinema.ticket_booking.dto.request.SocialLoginRequest;
import com.cinema.ticket_booking.dto.response.AuthResponse;


public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse socialLogin(SocialLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    void requestPasswordReset(String email);

    String verifyOtp(String email, String otp);

    void resetPassword(String token, String newPassword);
}
