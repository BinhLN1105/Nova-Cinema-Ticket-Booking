package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.AnalyticsResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AnalyticsService {
    AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId);
}
