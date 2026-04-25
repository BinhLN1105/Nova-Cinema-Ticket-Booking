package com.cinema.ticket_booking.service;

import java.time.Duration;
import java.util.List;

public interface SeatLockService {
    boolean lockSeat(String showtimeSeatId, String bookingId, Duration duration);

    void releaseSeat(String showtimeSeatId);

    void releaseSeats(List<String> showtimeSeatIds);

    boolean isSeatLocked(String showtimeSeatId);

    List<String> getLockedSeats(List<String> showtimeSeatIds);
}
