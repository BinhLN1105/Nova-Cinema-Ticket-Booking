package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPaymentUrl(UUID userId, PaymentRequest request);

    PaymentResponse handleVnpayCallback(Map<String, String> params);

    PaymentResponse getByBookingId(UUID bookingId);
}
