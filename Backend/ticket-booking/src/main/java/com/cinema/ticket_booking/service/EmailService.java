package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;

public interface EmailService {
    void sendPasswordResetOtpEmail(User user, String otp);
    void sendBookingConfirmationEmail(Booking booking);
    void sendCancellationEmail(Booking booking, String reason);
    void sendCancellationRequestEmail(String email, String customerName, String bookingCode, String movieTitle, java.util.UUID bookingId, String token);
}
