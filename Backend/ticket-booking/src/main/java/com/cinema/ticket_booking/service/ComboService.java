package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.ComboResponse;

import java.util.List;

public interface ComboService {

    List<ComboResponse> getAvailable();
}
