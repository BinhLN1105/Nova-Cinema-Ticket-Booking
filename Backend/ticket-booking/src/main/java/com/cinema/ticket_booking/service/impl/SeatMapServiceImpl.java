package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.mapper.SeatMapper;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.service.SeatMapService;
import com.cinema.ticket_booking.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatMapServiceImpl implements SeatMapService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeService showtimeService;
    private final SeatMapper seatMapper;

    @Override
    public SeatMapResponse getSeatMap(UUID showtimeId) {
        Showtime showtime = showtimeService.findById(showtimeId);

        var seats = showtimeSeatRepository
                .findByShowtimeIdWithSeat(showtimeId)
                .stream()
                .map(seatMapper::toSeatItem)
                .toList();

        return SeatMapResponse.builder()
                .showtimeId(showtimeId.toString())
                .totalRows(showtime.getScreen().getTotalRows())
                .totalCols(showtime.getScreen().getTotalCols())
                .seats(seats)
                .build();
    }
}
