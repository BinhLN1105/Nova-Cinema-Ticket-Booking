package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.GenreResponse;

import java.util.List;

public interface GenreService {

    List<GenreResponse> getAll();

    GenreResponse create(String name);
}
