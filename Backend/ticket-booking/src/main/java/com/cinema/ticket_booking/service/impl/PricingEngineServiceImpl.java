package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleTarget;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingEngineServiceImpl implements PricingEngineService {

    private final PromotionRepository promotionRepository;

    @Override
    public PricingResult calculateFinalSeatPrice(Showtime showtime, Seat seat, BigDecimal basePrice,
            List<PricingRule> activeRules, int ticketQty, int comboQty) {
        // 1. Áp dụng các quy tắc cơ bản (Thứ, Giờ, Loại ghế) - Luôn cộng dồn
        // Khuyến mãi (%) sẽ KHÔNG được tính ở bước chọn ghế để tránh gây nhầm lẫn.
        BigDecimal currentPrice = calculateBaseAdjustments(showtime, seat, basePrice, activeRules);

        // Trả về giá đã gồm phụ thu (VIP, Đôi, Ngày lễ,...) nhưng CHƯA trừ khuyến mãi
        return new PricingResult(currentPrice, BigDecimal.ZERO, null);
    }

    @Override
    public PricingResult calculateTimeAdjustedPrice(Showtime showtime, BigDecimal basePrice,
            List<PricingRule> activeRules) {
        BigDecimal timeAdjusted = calculateBaseAdjustments(showtime, null, basePrice, activeRules);
        return applyBestPromotion(timeAdjusted, showtime, activeRules, 1, 0);
    }

    @Override
    public PricingResult calculateBestOrderPromotion(Showtime showtime, int ticketQty, int comboQty,
            BigDecimal totalTicketOriginalPrice,
            BigDecimal totalComboOriginalPrice,
            List<PricingRule> activeRules) {

        // 1. Chỉ lọc các quy tắc loại PROMOTION đang hoạt động
        List<PricingRule> promotionRules = activeRules.stream()
                .filter(r -> r.getRuleType() == PricingRuleType.PROMOTION && r.getIsActive()
                        && r.getConditionValue() != null)
                .collect(Collectors.toList());

        if (promotionRules.isEmpty()) {
            return new PricingResult(totalTicketOriginalPrice.add(totalComboOriginalPrice), BigDecimal.ZERO, null);
        }

        // 2. Nhóm quy tắc theo ID Khuyến mãi
        Map<String, List<PricingRule>> promoGroups = promotionRules.stream()
                .collect(Collectors.groupingBy(PricingRule::getConditionValue));

        // 3. Sắp xếp các nhóm theo Priority tăng dần
        List<Map.Entry<String, List<PricingRule>>> sortedPromoGroups = promoGroups.entrySet().stream()
                .sorted((e1, e2) -> {
                    Integer p1 = Objects.requireNonNullElse(e1.getValue().get(0).getPriority(), 999);
                    Integer p2 = Objects.requireNonNullElse(e2.getValue().get(0).getPriority(), 999);
                    return p1.compareTo(p2);
                })
                .collect(Collectors.toList());

        BigDecimal bestPromoDiscount = BigDecimal.ZERO;
        String winningPromoName = null;
        Integer firstFoundPriority = null;

        for (Map.Entry<String, List<PricingRule>> entry : sortedPromoGroups) {
            String promotionIdStr = entry.getKey();
            List<PricingRule> groupRules = entry.getValue();
            Integer currentPriority = Objects.requireNonNullElse(groupRules.get(0).getPriority(), 999);

            // Nếu đã tìm thấy ít nhất 1 KM ở Priority cao hơn (số nhỏ hơn),
            // và KM hiện tại có Priority thấp hơn (số lớn hơn) thì dừng.
            if (firstFoundPriority != null && currentPriority > firstFoundPriority) {
                break;
            }

            try {
                UUID promotionId = UUID.fromString(promotionIdStr);
                Promotion promotion = promotionRepository.findById(promotionId).orElse(null);

                if (promotion == null || !promotion.getIsActive() || !isWithinPromotionDate(promotion, showtime)) {
                    continue;
                }

                BigDecimal currentPromoDiscount = BigDecimal.ZERO;
                boolean satisfiesBundle = true;

                for (PricingRule rule : groupRules) {
                    int minT = Objects.requireNonNullElse(rule.getMinTicketQty(), 0);
                    int minC = Objects.requireNonNullElse(rule.getMinComboQty(), 0);

                    if (ticketQty < minT || comboQty < minC) {
                        satisfiesBundle = false;
                        break;
                    }

                    BigDecimal ruleDiscount = calculateDiscountAmount(
                            rule.getTargetType() == PricingRuleTarget.TICKET ? totalTicketOriginalPrice
                                    : rule.getTargetType() == PricingRuleTarget.COMBO ? totalComboOriginalPrice
                                            : totalTicketOriginalPrice.add(totalComboOriginalPrice),
                            rule);

                    currentPromoDiscount = currentPromoDiscount.add(ruleDiscount);
                }

                if (satisfiesBundle && currentPromoDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    // Nếu tiền giảm lớn hơn hoặc cùng tiền nhưng Priority cao hơn (ít xảy ra do sắp
                    // xếp)
                    if (currentPromoDiscount.compareTo(bestPromoDiscount) > 0) {
                        bestPromoDiscount = currentPromoDiscount;
                        winningPromoName = promotion.getTitle();
                        firstFoundPriority = currentPriority; // Đánh dấu đã tìm thấy KM ở mức này
                    }
                }
            } catch (Exception e) {
                // Ignore invalid data
            }
        }

        BigDecimal finalTotal = totalTicketOriginalPrice.add(totalComboOriginalPrice).subtract(bestPromoDiscount);
        return new PricingResult(finalTotal.max(BigDecimal.ZERO), bestPromoDiscount, winningPromoName);
    }

    private boolean isWithinPromotionDate(Promotion promotion, Showtime showtime) {
        if (promotion == null || !promotion.getIsActive())
            return false;

        LocalDateTime showTime = showtime.getStartTime();
        // Một chương trình khuyến mãi nếu không để ngày bắt đầu/kết thúc thì coi như vô
        // hạn
        LocalDateTime startRange = promotion.getStartDate() != null ? promotion.getStartDate() : LocalDateTime.MIN;
        LocalDateTime endRange = promotion.getEndDate() != null
                ? promotion.getEndDate().toLocalDate().atTime(LocalTime.MAX)
                : LocalDateTime.MAX;

        boolean matches = !showTime.isBefore(startRange) && !showTime.isAfter(endRange);
        return matches;
    }

    private PricingResult applyBestPromotion(BigDecimal currentPrice, Showtime showtime, List<PricingRule> rules,
            int tQty, int cQty) {
        return calculateBestOrderPromotion(showtime, tQty, cQty, currentPrice, BigDecimal.ZERO, rules);
    }

    private BigDecimal calculateDiscountAmount(BigDecimal price, PricingRule rule) {
        // LUÔN lấy trị tuyệt đối cho Khuyến mãi vì mục đích cuối là để TRỪ tiền
        BigDecimal value = rule.getAdjustmentValue().abs();

        if (rule.getAdjustmentType() == AdjustmentType.PERCENTAGE) {
            BigDecimal multiplier = value.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            return price.multiply(multiplier);
        } else if (rule.getAdjustmentType() == AdjustmentType.FIXED_AMOUNT) {
            return value.min(price); // Không giảm quá giá gốc
        } else if (rule.getAdjustmentType() == AdjustmentType.MULTIPLIER) {
            // Hiếm khi dùng Multiplier cho khuyến mãi trừ tiền, nhưng nếu có thì
            // tính theo tỷ lệ (vd: 0.1 tương đương giảm 90%? Không, nên bỏ qua hoặc fix)
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateBaseAdjustments(Showtime showtime, Seat seat, BigDecimal basePrice,
            List<PricingRule> activeRules) {
        BigDecimal currentPrice = basePrice;

        List<PricingRule> baseRules = activeRules.stream()
                .filter(r -> r.getRuleType() != PricingRuleType.PROMOTION && r.getIsActive())
                .sorted((r1, r2) -> r1.getPriority().compareTo(r2.getPriority()))
                .collect(Collectors.toList());

        for (PricingRule rule : baseRules) {
            boolean applies = false;
            switch (rule.getRuleType()) {
                case DAY_OF_WEEK:
                    applies = showtime.getStartTime().getDayOfWeek().name().equalsIgnoreCase(rule.getConditionValue());
                    break;
                case TIME_FRAME:
                    String[] times = rule.getConditionValue().split("-");
                    if (times.length == 2) {
                        try {
                            LocalTime start = LocalTime.parse(times[0]);
                            LocalTime end = LocalTime.parse(times[1]);
                            LocalTime showStart = showtime.getStartTime().toLocalTime();
                            applies = !showStart.isBefore(start) && !showStart.isAfter(end);
                        } catch (Exception e) {
                        }
                    }
                    break;
                case SEAT_TYPE:
                    applies = seat != null && seat.getSeatType().name().equalsIgnoreCase(rule.getConditionValue());
                    break;
                case DATE_RANGE:
                    String[] dates = rule.getConditionValue().split(",");
                    if (dates.length == 2) {
                        try {
                            LocalDate start = LocalDate.parse(dates[0]);
                            LocalDate end = LocalDate.parse(dates[1]);
                            LocalDate showDate = showtime.getStartTime().toLocalDate();
                            applies = !showDate.isBefore(start) && !showDate.isAfter(end);
                        } catch (Exception e) {
                        }
                    }
                    break;
                default:
                    break;
            }

            if (applies) {
                switch (rule.getAdjustmentType()) {
                    case PERCENTAGE:
                        BigDecimal percentMultiplier = BigDecimal.ONE
                                .add(rule.getAdjustmentValue().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                        currentPrice = currentPrice.multiply(percentMultiplier);
                        break;
                    case MULTIPLIER:
                        currentPrice = currentPrice.multiply(rule.getAdjustmentValue());
                        break;
                    case FIXED_AMOUNT:
                    default:
                        currentPrice = currentPrice.add(rule.getAdjustmentValue());
                        break;
                }
            }
        }
        return currentPrice.max(BigDecimal.ZERO);
    }
}
