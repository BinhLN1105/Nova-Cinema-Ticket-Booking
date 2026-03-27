package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.mapper.SeatMapper;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.service.SeatMapService;
import com.cinema.ticket_booking.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cinema.ticket_booking.service.SystemConfigService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatMapServiceImpl implements SeatMapService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeService showtimeService;
    private final SeatMapper seatMapper;
    private final SystemConfigService systemConfigService;

    @Override
    public SeatMapResponse getSeatMap(UUID showtimeId) {
        Showtime showtime = showtimeService.findById(showtimeId);

        var seats = showtimeSeatRepository
                .findByShowtimeIdWithSeat(showtimeId)
                .stream()
                .map(seatMapper::toSeatItem)
                .toList();

        // Calculate actual hold time
        long minutesToStart = Duration.between(LocalDateTime.now(), showtime.getStartTime()).toMinutes();
        int defaultHold = systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10);
        int lateHold = systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3);

        int actualHoldMins = (minutesToStart <= 15 && minutesToStart >= -10) ? lateHold : defaultHold;

        int maxGridRow = seats.stream().mapToInt(SeatMapResponse.SeatItem::getGridRow).max().orElse(0);
        int maxGridCol = seats.stream().mapToInt(SeatMapResponse.SeatItem::getGridCol).max().orElse(0);

        return SeatMapResponse.builder()
                .showtimeId(showtimeId.toString())
                .totalRows(showtime.getScreen().getTotalRows())
                .totalCols(showtime.getScreen().getTotalCols())
                .maxGridRow(maxGridRow)
                .maxGridCol(maxGridCol)
                .seats(seats)
                .seatHoldMins(actualHoldMins)
                .build();
    }
}
