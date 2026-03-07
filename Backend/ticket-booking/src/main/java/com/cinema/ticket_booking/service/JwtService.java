package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.User;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken();

    boolean isTokenValid(String token);

    String extractUserId(String token);

    String extractRole(String token);

}
