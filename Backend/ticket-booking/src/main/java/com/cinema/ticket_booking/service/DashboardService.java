package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DashboardService {
    DashboardStatsResponse getStats(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId);
}
