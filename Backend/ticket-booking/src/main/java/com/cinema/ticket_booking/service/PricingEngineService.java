package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;

import java.math.BigDecimal;
import java.util.List;

public interface PricingEngineService {
    BigDecimal calculateFinalSeatPrice(Showtime showtime, Seat seat, BigDecimal basePrice, List<PricingRule> activeRules);
    BigDecimal calculateTimeAdjustedPrice(Showtime showtime, BigDecimal basePrice, List<PricingRule> activeRules);
}
