package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.Booking;

public interface EmailService {
    void sendCancellationConfirmEmail(Booking booking, String token);
}
