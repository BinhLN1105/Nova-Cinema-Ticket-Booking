package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface WalletService {
    PaymentResponse createTopUpUrl(UUID userId, BigDecimal amount, String returnUrlBase);
    void handleVnpayCallback(Map<String, String> params);
}
