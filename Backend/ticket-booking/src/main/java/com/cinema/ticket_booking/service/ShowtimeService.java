package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.model.Showtime;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    List<ShowtimeResponse> getByMovieAndDate(UUID movieId, LocalDate date);

    List<ShowtimeResponse> getByMovieCinemaAndDate(UUID movieId, UUID cinemaId, LocalDate date);

    ShowtimeResponse getById(UUID id);

    SeatMapResponse getSeatMap(UUID showtimeId);

    ShowtimeResponse create(ShowtimeRequest request);

    Showtime findById(UUID id);
}
