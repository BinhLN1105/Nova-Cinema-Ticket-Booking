package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.model.Screen;

import java.util.List;
import java.util.UUID;

public interface ScreenService {

    List<ScreenResponse> getByCinema(UUID cinemaId);

    ScreenResponse create(ScreenRequest request);

    ScreenResponse update(UUID id, ScreenRequest request);

    void delete(UUID id);

    Screen findById(UUID id);
}
