package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.model.Cinema;

import java.util.List;
import java.util.UUID;

public interface CinemaService {

    List<CinemaResponse> getAll(String city);

    CinemaResponse getById(UUID id);

    CinemaResponse create(CinemaRequest request);

    CinemaResponse update(UUID id, CinemaRequest request);

    void deactivate(UUID id);

    Cinema findById(UUID id); // Dùng cho nội bộ hoặc các service khác gọi sang
}
