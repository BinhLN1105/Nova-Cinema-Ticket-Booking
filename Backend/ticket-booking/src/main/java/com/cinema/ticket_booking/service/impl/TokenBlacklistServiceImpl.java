package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.service.JwtService;
import com.cinema.ticket_booking.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token_blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    @Override
    public void blacklist(String token) {
        long remainingMs = jwtService.getRemainingExpiration(token);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "blacklisted",
                    Duration.ofMillis(remainingMs));
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
