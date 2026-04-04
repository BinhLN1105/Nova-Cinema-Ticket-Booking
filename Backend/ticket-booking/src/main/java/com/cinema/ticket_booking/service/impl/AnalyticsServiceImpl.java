package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.AnalyticsResponse;
import com.cinema.ticket_booking.dto.response.AnalyticsResponse.*;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;

    @Override
    public AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId) {
        return AnalyticsResponse.builder()
                .occupancyByCinema(calculateOccupancy(startDate, endDate, cinemaId))
                .peakHours(calculatePeakHours(startDate, endDate, cinemaId))
                .revenueByCinema(calculateRevenueByCinema(startDate, endDate, cinemaId))
                .build();
    }

    private List<OccupancyData> calculateOccupancy(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId) {
        List<Object[]> rows = bookingRepository.getOccupancyInRange(startDate, endDate, cinemaId);
        return rows.stream()
                .map(row -> {
                    String name = (String) row[0];
                    long total = ((Number) row[1]).longValue();
                    long booked = ((Number) row[2]).longValue();
                    double rate = total > 0
                            ? BigDecimal.valueOf(booked * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return new OccupancyData(name, total, booked, rate);
                })
                .collect(Collectors.toList());
    }

    private List<PeakHourData> calculatePeakHours(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId) {
        List<Object[]> rows = bookingRepository.getPeakHoursInRange(startDate, endDate, cinemaId);
        Map<Integer, Long> hourCounts = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()));

        List<PeakHourData> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new PeakHourData(h, hourCounts.getOrDefault(h, 0L)));
        }
        return result;
    }

    private List<RevenueByCinema> calculateRevenueByCinema(LocalDateTime startDate, LocalDateTime endDate,
            UUID cinemaId) {
        List<Object[]> rows = bookingRepository.getRevenueByCinemaInRange(startDate, endDate, cinemaId);
        return rows.stream()
                .map(row -> new RevenueByCinema(
                        (String) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).intValue()))
                .collect(Collectors.toList());
    }
}
