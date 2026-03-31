package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;

public interface EmailService {
    void sendCancellationConfirmEmail(Booking booking, String token);
    void sendPasswordResetEmail(User user, String token);
}
