package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.enums.PricingRuleType;
import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.model.Promotion;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.PromotionRepository;
import com.cinema.ticket_booking.service.PricingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingEngineServiceImpl implements PricingEngineService {

    private final PromotionRepository promotionRepository;

    @Override
    public BigDecimal calculateFinalSeatPrice(Showtime showtime, Seat seat, BigDecimal basePrice, List<PricingRule> activeRules) {
        // 1. Calculate time adjusted price (Base Rules)
        BigDecimal timeAdjustedPrice = applyBaseTimeRules(basePrice, showtime, activeRules);

        // 2. Apply Seat Type rules (Base Rules)
        BigDecimal seatAdjustedPrice = applySeatRules(timeAdjustedPrice, seat, activeRules);

        // 3. Apply Promotion rules (Mutually Exclusive - pick max discount)
        return applyPromotionRules(seatAdjustedPrice, showtime, activeRules);
    }
    
    @Override
    public BigDecimal calculateTimeAdjustedPrice(Showtime showtime, BigDecimal basePrice, List<PricingRule> activeRules) {
        BigDecimal timeAdjusted = applyBaseTimeRules(basePrice, showtime, activeRules);
        return applyPromotionRules(timeAdjusted, showtime, activeRules);
    }

    private BigDecimal applyBaseTimeRules(BigDecimal basePrice, Showtime showtime, List<PricingRule> rules) {
        BigDecimal price = basePrice;
        for (PricingRule rule : rules) {
            boolean apply = false;
            switch (rule.getRuleType()) {
                case DAY_OF_WEEK:
                    if (showtime.getStartTime().getDayOfWeek().name().equalsIgnoreCase(rule.getConditionValue())) {
                        apply = true;
                    }
                    break;
                case TIME_FRAME:
                    String[] times = rule.getConditionValue().split("-");
                    if (times.length == 2) {
                        try {
                            LocalTime start = LocalTime.parse(times[0]);
                            LocalTime end = LocalTime.parse(times[1]);
                            LocalTime showTimeTime = showtime.getStartTime().toLocalTime();
                            if (!showTimeTime.isBefore(start) && !showTimeTime.isAfter(end)) {
                                apply = true;
                            }
                        } catch (Exception e) {}
                    }
                    break;
                case DATE_RANGE:
                    String[] dates = rule.getConditionValue().split(",");
                    if (dates.length == 2) {
                        try {
                            LocalDate startDate = LocalDate.parse(dates[0]);
                            LocalDate endDate = LocalDate.parse(dates[1]);
                            LocalDate showDate = showtime.getStartTime().toLocalDate();
                            if (!showDate.isBefore(startDate) && !showDate.isAfter(endDate)) {
                                apply = true;
                            }
                        } catch (Exception e) {}
                    }
                    break;
                default:
                    break;
            }

            if (apply) {
                price = calculateAdjustment(price, rule);
            }
        }
        return price;
    }

    private BigDecimal applySeatRules(BigDecimal timeAdjustedPrice, Seat seat, List<PricingRule> rules) {
        BigDecimal price = timeAdjustedPrice;
        for (PricingRule rule : rules) {
            if (rule.getRuleType() == PricingRuleType.SEAT_TYPE) {
                if (seat.getSeatType().name().equalsIgnoreCase(rule.getConditionValue())) {
                    price = calculateAdjustment(price, rule);
                }
            }
        }
        return price;
    }

    private BigDecimal applyPromotionRules(BigDecimal currentPrice, Showtime showtime, List<PricingRule> rules) {
        BigDecimal maxDiscountAmount = BigDecimal.ZERO;
        boolean hasPromotion = false;

        for (PricingRule rule : rules) {
            if (rule.getRuleType() == PricingRuleType.PROMOTION) {
                try {
                    UUID promotionId = UUID.fromString(rule.getConditionValue());
                    Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
                    
                    if (promotion != null && promotion.getIsActive()) {
                        // Check date overlaps
                        boolean withinDate = true;
                        if (promotion.getStartDate() != null && showtime.getStartTime().isBefore(promotion.getStartDate())) {
                            withinDate = false;
                        }
                        if (promotion.getEndDate() != null && showtime.getStartTime().isAfter(promotion.getEndDate())) {
                            withinDate = false;
                        }
                        
                        if (withinDate) {
                            hasPromotion = true;
                            BigDecimal potentialAdjustedPrice = calculateAdjustment(currentPrice, rule);
                            // We expect potentialAdjustedPrice < currentPrice. The discount is currentPrice - potentialAdjustedPrice
                            BigDecimal discount = currentPrice.subtract(potentialAdjustedPrice);
                            if (discount.compareTo(maxDiscountAmount) > 0) {
                                maxDiscountAmount = discount;
                            }
                        }
                    }
                } catch (Exception e) {
                   // Invalid UUID or missing promotion, ignore
                }
            }
        }

        if (hasPromotion) {
            // Apply the best discount found
            return currentPrice.subtract(maxDiscountAmount).max(BigDecimal.ZERO);
        }
        return currentPrice;
    }

    private BigDecimal calculateAdjustment(BigDecimal currentPrice, PricingRule rule) {
        switch (rule.getAdjustmentType()) {
            case PERCENTAGE:
                BigDecimal multiplier = BigDecimal.ONE.add(rule.getAdjustmentValue().divide(BigDecimal.valueOf(100)));
                return currentPrice.multiply(multiplier);
            case FIXED_AMOUNT:
                return currentPrice.add(rule.getAdjustmentValue());
            case MULTIPLIER:
                return currentPrice.multiply(rule.getAdjustmentValue());
            default:
                return currentPrice;
        }
    }
}
