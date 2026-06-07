package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.CheckInResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(UUID userId, BookingRequest request);

    BookingResponse calculateQuote(UUID userId, BookingRequest request);

    PageResponse<BookingResponse.Summary> getMyBookings(UUID userId, Pageable pageable);

    BookingResponse getDetail(UUID userId, UUID bookingId);

    CheckInResponse checkIn(User staff, String qrCode);

    void confirmPaid(UUID bookingId);

    void cancelBooking(User actionUser, UUID bookingId);

    void cancelRequest(UUID userId, UUID bookingId);

    void cancelConfirm(String token, UUID bookingId);

    String getCancelToken(UUID userId, UUID bookingId);

    

    Booking findById(UUID id);

    UUID getEligibleBookingForReview(UUID userId, UUID movieId);
}
