package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;

import java.math.BigDecimal;
import java.util.List;

public interface PricingEngineService {
    PricingResult calculateFinalSeatPrice(Showtime showtime, Seat seat, BigDecimal basePrice, List<PricingRule> activeRules, int ticketQty, int comboQty);
    PricingResult calculateTimeAdjustedPrice(Showtime showtime, BigDecimal basePrice, List<PricingRule> activeRules);
    
    // Thuật toán 'Nhà Vô Địch' cho toàn bộ đơn hàng (Vé + Combo)
    PricingResult calculateBestOrderPromotion(Showtime showtime, int ticketQty, int comboQty, 
                                            BigDecimal totalTicketOriginalPrice, 
                                            BigDecimal totalComboOriginalPrice, 
                                            List<PricingRule> activeRules);
}

