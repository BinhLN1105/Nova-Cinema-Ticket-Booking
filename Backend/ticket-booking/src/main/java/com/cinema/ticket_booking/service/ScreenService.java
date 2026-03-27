package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.request.ScreenSeatLayoutRequest;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Seat;

import java.util.List;
import java.util.UUID;

public interface ScreenService {

    List<ScreenResponse> getByCinema(UUID cinemaId);

    List<ScreenResponse> getByCinemaForAdmin(UUID cinemaId);

    ScreenResponse create(ScreenRequest request);

    ScreenResponse update(UUID id, ScreenRequest request);

    void delete(UUID id, String type);

    Screen findById(UUID id);

    /** Lưu bố trí ghế tuỳ chỉnh (custom layout) */
    void saveCustomLayout(ScreenSeatLayoutRequest request);

    /** Lấy danh sách ghế active của một phòng chiếu */
    List<Seat> getSeats(UUID screenId);
}
