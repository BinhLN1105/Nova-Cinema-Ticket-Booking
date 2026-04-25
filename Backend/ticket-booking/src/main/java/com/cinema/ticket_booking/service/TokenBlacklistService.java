package com.cinema.ticket_booking.service;

/**
 * Service to manage JWT token blacklisting via Redis.
 * When a user logs out, their access token is added to the blacklist
 * with a TTL equal to its remaining lifetime.
 */
public interface TokenBlacklistService {

    /**
     * Add a JWT access token to the blacklist.
     * @param token the JWT access token string
     */
    void blacklist(String token);

    /**
     * Check if a JWT access token has been blacklisted.
     * @param token the JWT access token string
     * @return true if the token is blacklisted
     */
    boolean isBlacklisted(String token);
}
