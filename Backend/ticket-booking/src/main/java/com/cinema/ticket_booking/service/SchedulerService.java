package com.cinema.ticket_booking.service;

public interface SchedulerService {

    /**
     * Giải phóng ghế LOCKED đã hết hạn giữ chỗ.
     */
    void releaseExpiredSeatLocks();

    /**
     * Chuyển booking PENDING hết hạn → EXPIRED, đồng thời giải phóng ghế.
     */
    void expireOverdueBookings();

    /**
     * Dọn dẹp refresh token hết hạn trong DB.
     */
    void cleanExpiredRefreshTokens();
}
