package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SeatLockServiceImpl implements SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "seat_lock:";

    @Override
    public boolean lockSeat(String showtimeSeatId, String bookingId, Duration duration) {
        String key = LOCK_PREFIX + showtimeSeatId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, bookingId, duration);
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void releaseSeat(String showtimeSeatId) {
        redisTemplate.delete(LOCK_PREFIX + showtimeSeatId);
    }

    @Override
    public void releaseSeats(List<String> showtimeSeatIds) {
        List<String> keys = showtimeSeatIds.stream()
                .map(id -> LOCK_PREFIX + id)
                .collect(Collectors.toList());
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean isSeatLocked(String showtimeSeatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + showtimeSeatId));
    }

    @Override
    public List<String> getLockedSeats(List<String> showtimeSeatIds) {
        if (showtimeSeatIds == null || showtimeSeatIds.isEmpty())
            return Collections.emptyList();

        List<String> keys = showtimeSeatIds.stream()
                .map(id -> LOCK_PREFIX + id)
                .collect(Collectors.toList());

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null)
            return Collections.emptyList();

        return IntStream.range(0, showtimeSeatIds.size())
                .filter(i -> values.get(i) != null)
                .mapToObj(showtimeSeatIds::get)
                .collect(Collectors.toList());
    }
}
