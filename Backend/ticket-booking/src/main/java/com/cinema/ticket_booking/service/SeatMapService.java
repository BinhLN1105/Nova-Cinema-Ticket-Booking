package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.SeatMapResponse;

import java.util.UUID;

public interface SeatMapService {

    /**
     * Trả về toàn bộ sơ đồ ghế của 1 suất chiếu.
     * Android dùng để render màn hình chọn ghế.
     */
    SeatMapResponse getSeatMap(UUID showtimeId);
}
