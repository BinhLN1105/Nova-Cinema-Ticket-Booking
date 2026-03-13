package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.OverrideSeatPriceRequest;
import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.model.Showtime;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    List<ShowtimeResponse> getByMovieAndDate(UUID movieId, LocalDate date);

    List<ShowtimeResponse> getByMovieCinemaAndDate(UUID movieId, UUID cinemaId, LocalDate date);

    ShowtimeResponse getById(UUID id);

    SeatMapResponse getSeatMap(UUID showtimeId);

    ShowtimeResponse create(ShowtimeRequest request);

    PageResponse<ShowtimeResponse> adminList(Pageable pageable,
                                              String cinemaId, java.time.LocalDate date);

    void overrideSeatPrices(UUID showtimeId, OverrideSeatPriceRequest request);

    void delete(UUID id);

    Showtime findById(UUID id);
}
