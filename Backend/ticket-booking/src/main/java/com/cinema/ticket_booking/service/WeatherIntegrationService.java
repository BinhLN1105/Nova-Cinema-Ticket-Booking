package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.WeatherShowtimeResponse;
import java.util.UUID;

public interface WeatherIntegrationService {

    /**
     * Lấy dự báo thời tiết tại rạp chiếu phim tại khung giờ bắt đầu của suất chiếu.
     * Sử dụng DB Cache 4 giờ để tránh vượt quá quota Weather API bên ngoài.
     * 
     * @param showtimeId ID của suất chiếu.
     * @return DTO chứa thông tin thời tiết chi tiết phục vụ AI/Client.
     */
    WeatherShowtimeResponse getWeatherForShowtime(UUID showtimeId);
}
