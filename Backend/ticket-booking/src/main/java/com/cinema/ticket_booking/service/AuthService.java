package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.LoginRequest;
import com.cinema.ticket_booking.dto.request.RefreshTokenRequest;
import com.cinema.ticket_booking.dto.request.RegisterRequest;
import com.cinema.ticket_booking.dto.request.SocialLoginRequest;
import com.cinema.ticket_booking.dto.response.AuthResponse;
import com.cinema.ticket_booking.model.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse socialLogin(SocialLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(User currentUser);

}
