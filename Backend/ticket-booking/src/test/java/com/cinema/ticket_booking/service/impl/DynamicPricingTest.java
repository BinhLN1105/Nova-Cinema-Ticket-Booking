package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleTarget;
import com.cinema.ticket_booking.enums.PricingRuleType;
import com.cinema.ticket_booking.enums.SeatType;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PricingRuleMapper;
import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.model.Promotion;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.PricingRuleRepository;
import com.cinema.ticket_booking.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicPricingTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000");

    private PromotionRepository promotionRepository;
    private PricingRuleRepository pricingRuleRepository;
    private PricingEngineServiceImpl pricingEngineService;
    private PricingRuleServiceImpl pricingRuleService;

    private Showtime showtime;
    private Seat seat;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        pricingRuleRepository = mock(PricingRuleRepository.class);

        pricingEngineService = new PricingEngineServiceImpl(promotionRepository);
        pricingRuleService = new PricingRuleServiceImpl(
                pricingRuleRepository,
                promotionRepository,
                new PricingRuleMapper()
        );

        showtime = mock(Showtime.class);
        seat = mock(Seat.class);

        when(showtime.getStartTime()).thenReturn(LocalDateTime.of(2026, 6, 22, 10, 0));
        when(seat.getSeatType()).thenReturn(SeatType.STANDARD);
    }

    @Test
    void weekdayStandardSeatWithoutRules_shouldKeepBasePrice() {
        PricingResult result = calculateSeatPrice(List.of());

        assertMoney("100000", result.finalPrice());
        assertMoney("0", result.discountAmount());
        assertNull(result.appliedPromotionName());
    }

    @Test
    void saturday_shouldApplyWeekendPercentageSurcharge() {
        at(2026, 6, 27, 10, 0);

        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.DAY_OF_WEEK, "SATURDAY",
                        AdjustmentType.PERCENTAGE, "20", 1)
        ));

        assertMoney("120000", result.finalPrice());
    }

    @Test
    void sunday_shouldApplyWeekendFixedSurcharge() {
        at(2026, 6, 28, 10, 0);

        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.DAY_OF_WEEK, "SUNDAY",
                        AdjustmentType.FIXED_AMOUNT, "15000", 1)
        ));

        assertMoney("115000", result.finalPrice());
    }

    @Test
    void mondayWithSaturdayRule_shouldKeepBasePrice() {
        at(2026, 6, 22, 10, 0);

        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.DAY_OF_WEEK, "SATURDAY",
                        AdjustmentType.PERCENTAGE, "20", 1)
        ));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void oneMinuteBeforePeakStart_shouldNotApplyTimeSurcharge() {
        at(2026, 6, 22, 17, 59);

        PricingResult result = calculateSeatPrice(List.of(peakRule()));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void exactlyAtPeakStart_shouldApplyTimeSurcharge() {
        at(2026, 6, 22, 18, 0);

        PricingResult result = calculateSeatPrice(List.of(peakRule()));

        assertMoney("130000", result.finalPrice());
    }

    @Test
    void oneMinuteAfterPeakStart_shouldApplyTimeSurcharge() {
        at(2026, 6, 22, 18, 1);

        PricingResult result = calculateSeatPrice(List.of(peakRule()));

        assertMoney("130000", result.finalPrice());
    }

    @Test
    void exactlyAtPeakEnd_shouldApplyTimeSurcharge() {
        at(2026, 6, 22, 21, 0);

        PricingResult result = calculateSeatPrice(List.of(peakRule()));

        assertMoney("130000", result.finalPrice());
    }

    @Test
    void oneMinuteAfterPeakEnd_shouldNotApplyTimeSurcharge() {
        at(2026, 6, 22, 21, 1);

        PricingResult result = calculateSeatPrice(List.of(peakRule()));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void malformedTimeFrames_shouldBeIgnored() {
        at(2026, 6, 22, 19, 0);

        PricingRule wrongLength = rule(
                PricingRuleType.TIME_FRAME, "18:00",
                AdjustmentType.FIXED_AMOUNT, "30000", 1
        );
        PricingRule invalidTime = rule(
                PricingRuleType.TIME_FRAME, "invalid-21:00",
                AdjustmentType.FIXED_AMOUNT, "30000", 2
        );

        PricingResult result = calculateSeatPrice(List.of(wrongLength, invalidTime));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void vipSeat_shouldApplySeatTypeSurcharge() {
        when(seat.getSeatType()).thenReturn(SeatType.VIP);

        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.SEAT_TYPE, "VIP",
                        AdjustmentType.FIXED_AMOUNT, "25000", 1)
        ));

        assertMoney("125000", result.finalPrice());
    }

    @Test
    void coupleSeat_shouldApplySweetboxSurcharge() {
        when(seat.getSeatType()).thenReturn(SeatType.COUPLE);

        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.SEAT_TYPE, "COUPLE",
                        AdjustmentType.FIXED_AMOUNT, "40000", 1)
        ));

        assertMoney("140000", result.finalPrice());
    }

    @Test
    void standardSeatWithVipRule_shouldNotApplySurcharge() {
        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.SEAT_TYPE, "VIP",
                        AdjustmentType.FIXED_AMOUNT, "25000", 1)
        ));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void weekendPeakVip_shouldAccumulateRulesByPriority() {
        at(2026, 6, 27, 18, 30);
        when(seat.getSeatType()).thenReturn(SeatType.VIP);

        PricingRule weekend = rule(
                PricingRuleType.DAY_OF_WEEK, "SATURDAY",
                AdjustmentType.PERCENTAGE, "20", 1
        );
        PricingRule peak = rule(
                PricingRuleType.TIME_FRAME, "18:00-21:00",
                AdjustmentType.FIXED_AMOUNT, "30000", 2
        );
        PricingRule vip = rule(
                PricingRuleType.SEAT_TYPE, "VIP",
                AdjustmentType.MULTIPLIER, "1.5", 3
        );

        PricingResult result = calculateSeatPrice(List.of(vip, peak, weekend));

        assertMoney("225000", result.finalPrice());
    }

    @Test
    void inactiveAndPromotionRules_shouldNotChangeSeatPrice() {
        PricingRule inactive = rule(
                PricingRuleType.DAY_OF_WEEK, "MONDAY",
                AdjustmentType.FIXED_AMOUNT, "50000", 1
        );
        inactive.setIsActive(false);

        PricingRule promotion = promotionRule(
                UUID.randomUUID(), AdjustmentType.PERCENTAGE,
                "50", PricingRuleTarget.TICKET, 0, 0, 2
        );

        PricingResult result = calculateSeatPrice(List.of(inactive, promotion));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void dateRange_shouldIncludeBothBoundaryDates() {
        at(2026, 6, 30, 12, 0);

        PricingRule dateRule = rule(
                PricingRuleType.DATE_RANGE, "2026-06-30,2026-07-01",
                AdjustmentType.FIXED_AMOUNT, "10000", 1
        );

        PricingResult result = calculateSeatPrice(List.of(dateRule));

        assertMoney("110000", result.finalPrice());
    }

    @Test
    void malformedDateRanges_shouldBeIgnored() {
        PricingRule wrongLength = rule(
                PricingRuleType.DATE_RANGE, "2026-06-30",
                AdjustmentType.FIXED_AMOUNT, "10000", 1
        );
        PricingRule invalidDate = rule(
                PricingRuleType.DATE_RANGE, "bad-date,2026-07-01",
                AdjustmentType.FIXED_AMOUNT, "10000", 2
        );

        PricingResult result = calculateSeatPrice(List.of(wrongLength, invalidDate));

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void negativeAdjustment_shouldNeverReturnNegativePrice() {
        PricingResult result = calculateSeatPrice(List.of(
                rule(PricingRuleType.DAY_OF_WEEK, "MONDAY",
                        AdjustmentType.FIXED_AMOUNT, "-150000", 1)
        ));

        assertMoney("0", result.finalPrice());
    }

    @Test
    void calculateTimeAdjustedPrice_withoutPromotion_shouldReturnAdjustedPrice() {
        at(2026, 6, 22, 18, 30);

        PricingResult result = pricingEngineService.calculateTimeAdjustedPrice(
                showtime,
                BASE_PRICE,
                List.of(
                        peakRule(),
                        promotionRuleWithCondition(null, AdjustmentType.PERCENTAGE,
                                "20", PricingRuleTarget.TICKET, 0, 0, 2)
                )
        );

        assertMoney("130000", result.finalPrice());
        assertMoney("0", result.discountAmount());
        assertNull(result.appliedPromotionName());
    }

    @Test
    void validTicketPercentagePromotion_shouldApplyDiscount() {
        UUID promoId = UUID.randomUUID();
        at(2026, 6, 22, 19, 0);
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Ticket 10%", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 2, 0,
                new BigDecimal("200000"), BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "-10", PricingRuleTarget.TICKET, 0, 0, 1
                ))
        );

        assertMoney("180000", result.finalPrice());
        assertMoney("20000", result.discountAmount());
        assertEquals("Ticket 10%", result.appliedPromotionName());
    }

    @Test
    void validComboFixedPromotion_shouldApplyDiscount() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Combo deal", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 1,
                new BigDecimal("100000"), new BigDecimal("80000"),
                List.of(promotionRule(
                        promoId, AdjustmentType.FIXED_AMOUNT,
                        "-30000", PricingRuleTarget.COMBO, 0, 1, 1
                ))
        );

        assertMoney("150000", result.finalPrice());
        assertMoney("30000", result.discountAmount());
    }

    @Test
    void orderTotalPromotion_withNullMinimumsAndNullDates_shouldApply() {
        UUID promoId = UUID.randomUUID();
        Promotion promotion = Promotion.builder()
                .id(promoId)
                .title("Unlimited promotion")
                .isActive(true)
                .startDate(null)
                .endDate(null)
                .build();
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(promotion));

        PricingRule rule = promotionRule(
                promoId, AdjustmentType.PERCENTAGE,
                "25", PricingRuleTarget.ORDER_TOTAL, 0, 0, 1
        );
        rule.setMinTicketQty(null);
        rule.setMinComboQty(null);
        rule.setPriority(null);

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 1,
                new BigDecimal("100000"), new BigDecimal("100000"),
                List.of(rule)
        );

        assertMoney("150000", result.finalPrice());
        assertMoney("50000", result.discountAmount());
    }

    @Test
    void insufficientTicketQuantity_shouldRejectBundlePromotion() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Two tickets", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                new BigDecimal("100000"), BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "20", PricingRuleTarget.TICKET, 2, 0, 1
                ))
        );

        assertMoney("100000", result.finalPrice());
        assertMoney("0", result.discountAmount());
    }

    @Test
    void insufficientComboQuantity_shouldRejectBundlePromotion() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Ticket and combo", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 2, 0,
                new BigDecimal("200000"), new BigDecimal("50000"),
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "20", PricingRuleTarget.ORDER_TOTAL, 2, 1, 1
                ))
        );

        assertMoney("250000", result.finalPrice());
        assertMoney("0", result.discountAmount());
    }

    @Test
    void invalidPromotionUuid_shouldBeIgnored() {
        PricingRule invalid = promotionRuleWithCondition(
                "not-a-uuid", AdjustmentType.PERCENTAGE,
                "50", PricingRuleTarget.TICKET, 0, 0, 1
        );

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(invalid)
        );

        assertMoney("100000", result.finalPrice());
        assertMoney("0", result.discountAmount());
    }

    @Test
    void missingPromotionEntity_shouldBeIgnored() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId)).thenReturn(Optional.empty());

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "50", PricingRuleTarget.TICKET, 0, 0, 1
                ))
        );

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void inactivePromotionEntity_shouldBeIgnored() {
        UUID promoId = UUID.randomUUID();
        Promotion promotion = activePromotion(promoId, "Inactive", 2026, 6, 1, 2026, 6, 30);
        promotion.setIsActive(false);
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(promotion));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "50", PricingRuleTarget.TICKET, 0, 0, 1
                ))
        );

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void promotionOutsideShowtimeDate_shouldBeIgnored() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Expired", 2026, 5, 1, 2026, 5, 31)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.PERCENTAGE,
                        "50", PricingRuleTarget.TICKET, 0, 0, 1
                ))
        );

        assertMoney("100000", result.finalPrice());
    }

    @Test
    void samePriorityPromotions_shouldChooseLargerDiscount() {
        UUID smallId = UUID.randomUUID();
        UUID largeId = UUID.randomUUID();

        when(promotionRepository.findById(smallId))
                .thenReturn(Optional.of(activePromotion(smallId, "Small", 2026, 6, 1, 2026, 6, 30)));
        when(promotionRepository.findById(largeId))
                .thenReturn(Optional.of(activePromotion(largeId, "Large", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(
                        promotionRule(smallId, AdjustmentType.FIXED_AMOUNT,
                                "10000", PricingRuleTarget.TICKET, 0, 0, 1),
                        promotionRule(largeId, AdjustmentType.FIXED_AMOUNT,
                                "30000", PricingRuleTarget.TICKET, 0, 0, 1)
                )
        );

        assertMoney("70000", result.finalPrice());
        assertMoney("30000", result.discountAmount());
        assertEquals("Large", result.appliedPromotionName());
    }

    @Test
    void lowerPriorityPromotion_shouldNotBeEvaluatedAfterWinner() {
        UUID highPriorityId = UUID.randomUUID();
        UUID lowPriorityId = UUID.randomUUID();

        when(promotionRepository.findById(highPriorityId))
                .thenReturn(Optional.of(activePromotion(highPriorityId, "Priority one", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(
                        promotionRule(lowPriorityId, AdjustmentType.FIXED_AMOUNT,
                                "90000", PricingRuleTarget.TICKET, 0, 0, 2),
                        promotionRule(highPriorityId, AdjustmentType.FIXED_AMOUNT,
                                "10000", PricingRuleTarget.TICKET, 0, 0, 1)
                )
        );

        assertMoney("90000", result.finalPrice());
        verify(promotionRepository, never()).findById(lowPriorityId);
    }

    @Test
    void multiplierPromotion_shouldProduceNoDiscount() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Multiplier", 2026, 6, 1, 2026, 6, 30)));

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(promotionRule(
                        promoId, AdjustmentType.MULTIPLIER,
                        "0.5", PricingRuleTarget.TICKET, 0, 0, 1
                ))
        );

        assertMoney("100000", result.finalPrice());
        assertMoney("0", result.discountAmount());
    }

    @Test
    void multipleFixedRulesInOnePromotion_shouldClampFinalTotalToZero() {
        UUID promoId = UUID.randomUUID();
        when(promotionRepository.findById(promoId))
                .thenReturn(Optional.of(activePromotion(promoId, "Free order", 2026, 6, 1, 2026, 6, 30)));

        PricingRule first = promotionRule(
                promoId, AdjustmentType.FIXED_AMOUNT,
                "100000", PricingRuleTarget.ORDER_TOTAL, 0, 0, 1
        );
        PricingRule second = promotionRule(
                promoId, AdjustmentType.FIXED_AMOUNT,
                "100000", PricingRuleTarget.ORDER_TOTAL, 0, 0, 1
        );

        PricingResult result = pricingEngineService.calculateBestOrderPromotion(
                showtime, 1, 0,
                BASE_PRICE, BigDecimal.ZERO,
                List.of(first, second)
        );

        assertMoney("0", result.finalPrice());
        assertMoney("200000", result.discountAmount());
    }

    @Test
    void createPricingRule_shouldSaveAndMapResponse() {
        PricingRuleRequest request = request(
                "VIP surcharge", PricingRuleType.SEAT_TYPE,
                "VIP", AdjustmentType.FIXED_AMOUNT, "25000"
        );
        UUID generatedId = UUID.randomUUID();

        doAnswer(invocation -> {
            PricingRule saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return saved;
        }).when(pricingRuleRepository).save(any(PricingRule.class));

        PricingRuleResponse response = pricingRuleService.create(request);

        assertEquals(generatedId.toString(), response.getId());
        assertEquals("VIP", response.getConditionValue());
        assertTrue(response.getConditionDisplay().contains("VIP"));
        verify(pricingRuleRepository).save(any(PricingRule.class));
    }

    @Test
    void updatePricingRule_shouldModifyExistingEntity() {
        UUID id = UUID.randomUUID();
        PricingRule existing = identifiedRule(
                id, "Old", PricingRuleType.DAY_OF_WEEK,
                "MONDAY", AdjustmentType.FIXED_AMOUNT, "1000", 1
        );
        PricingRuleRequest request = request(
                "Updated", PricingRuleType.TIME_FRAME,
                "18:00-21:00", AdjustmentType.PERCENTAGE, "10"
        );

        when(pricingRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(pricingRuleRepository.save(existing)).thenReturn(existing);

        PricingRuleResponse response = pricingRuleService.update(id, request);

        assertEquals("Updated", response.getName());
        assertEquals(PricingRuleType.TIME_FRAME, response.getRuleType());
        assertTrue(response.getConditionDisplay().contains("18:00-21:00"));
        verify(pricingRuleRepository).save(existing);
    }

    @Test
    void toggleActive_shouldInvertAndSaveRule() {
        UUID id = UUID.randomUUID();
        PricingRule existing = identifiedRule(
                id, "Rule", PricingRuleType.DAY_OF_WEEK,
                "MONDAY", AdjustmentType.FIXED_AMOUNT, "1000", 1
        );
        existing.setIsActive(true);

        when(pricingRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(pricingRuleRepository.save(existing)).thenReturn(existing);

        PricingRuleResponse response = pricingRuleService.toggleActive(id);

        assertFalse(response.getIsActive());
        verify(pricingRuleRepository).save(existing);
    }

    @Test
    void deletePricingRule_shouldDelegateToRepository() {
        UUID id = UUID.randomUUID();

        pricingRuleService.delete(id);

        verify(pricingRuleRepository).deleteById(id);
    }

    @Test
    void getByIdPromotion_shouldEnrichPromotionDetails() {
        UUID id = UUID.randomUUID();
        UUID promoId = UUID.randomUUID();
        PricingRule rule = identifiedPromotionRule(id, promoId, 1);
        Promotion promotion = activePromotion(promoId, "Student deal", 2026, 6, 1, 2026, 6, 30);

        when(pricingRuleRepository.findById(id)).thenReturn(Optional.of(rule));
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(promotion));

        PricingRuleResponse response = pricingRuleService.getById(id);

        assertEquals("Student deal", response.getPromotionTitle());
        assertEquals(promotion.getStartDate(), response.getStartDate());
        assertEquals(promotion.getEndDate(), response.getEndDate());
    }

    @Test
    void getByIdInvalidPromotionId_shouldReturnInvalidDisplay() {
        UUID id = UUID.randomUUID();
        PricingRule rule = identifiedRule(
                id, "Invalid promo", PricingRuleType.PROMOTION,
                "bad-id", AdjustmentType.PERCENTAGE, "10", 1
        );

        when(pricingRuleRepository.findById(id)).thenReturn(Optional.of(rule));

        PricingRuleResponse response = pricingRuleService.getById(id);

        assertTrue(response.getConditionDisplay().contains("ID"));
    }

    @Test
    void getAll_shouldFormatRuleConditionsAndLoadPromotionsInBatch() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID promoId = UUID.randomUUID();

        PricingRule promotionRule = identifiedPromotionRule(UUID.randomUUID(), promoId, 1);
        PricingRule dayRule = identifiedRule(
                UUID.randomUUID(), "Day", PricingRuleType.DAY_OF_WEEK,
                "SATURDAY", AdjustmentType.FIXED_AMOUNT, "1000", 2
        );
        PricingRule timeRule = identifiedRule(
                UUID.randomUUID(), "Time", PricingRuleType.TIME_FRAME,
                "18:00-21:00", AdjustmentType.FIXED_AMOUNT, "1000", 3
        );
        PricingRule seatRule = identifiedRule(
                UUID.randomUUID(), "Seat", PricingRuleType.SEAT_TYPE,
                "VIP", AdjustmentType.FIXED_AMOUNT, "1000", 4
        );
        PricingRule dateRule = identifiedRule(
                UUID.randomUUID(), "Date", PricingRuleType.DATE_RANGE,
                "2026-06-01,2026-06-30", AdjustmentType.FIXED_AMOUNT, "1000", 5
        );
        PricingRule invalidDateRule = identifiedRule(
                UUID.randomUUID(), "Bad date", PricingRuleType.DATE_RANGE,
                "invalid", AdjustmentType.FIXED_AMOUNT, "1000", 6
        );
        PricingRule invalidPromotion = identifiedRule(
                UUID.randomUUID(), "Bad promotion", PricingRuleType.PROMOTION,
                "bad-id", AdjustmentType.PERCENTAGE, "10", 7
        );

        List<PricingRule> rules = List.of(
                promotionRule, dayRule, timeRule, seatRule,
                dateRule, invalidDateRule, invalidPromotion
        );

        when(pricingRuleRepository.findAllByOrderByPriorityAsc(pageable))
                .thenReturn(new PageImpl<>(rules, pageable, rules.size()));
        when(promotionRepository.findAllById(any()))
                .thenReturn(List.of(activePromotion(
                        promoId, "Batch promotion",
                        2026, 6, 1, 2026, 6, 30
                )));

        PageResponse<PricingRuleResponse> response = pricingRuleService.getAll(pageable);

        assertEquals(7, response.getContent().size());
        assertEquals("Batch promotion", response.getContent().get(0).getPromotionTitle());
        assertTrue(response.getContent().get(1).getConditionDisplay().contains("SATURDAY"));
        assertTrue(response.getContent().get(2).getConditionDisplay().contains("18:00-21:00"));
        assertTrue(response.getContent().get(3).getConditionDisplay().contains("VIP"));
        assertTrue(response.getContent().get(4).getConditionDisplay().contains("2026-06-01"));
        assertEquals("invalid", response.getContent().get(5).getConditionDisplay());
        assertTrue(response.getContent().get(6).getConditionDisplay().contains("ID"));
    }

    @Test
    void getByIdMissingRule_shouldThrowResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(pricingRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pricingRuleService.getById(id));
    }

    private PricingResult calculateSeatPrice(List<PricingRule> rules) {
        return pricingEngineService.calculateFinalSeatPrice(
                showtime, seat, BASE_PRICE, rules, 1, 0
        );
    }

    private void at(int year, int month, int day, int hour, int minute) {
        when(showtime.getStartTime())
                .thenReturn(LocalDateTime.of(year, month, day, hour, minute));
    }

    private PricingRule peakRule() {
        return rule(
                PricingRuleType.TIME_FRAME, "18:00-21:00",
                AdjustmentType.FIXED_AMOUNT, "30000", 1
        );
    }

    private PricingRule rule(
            PricingRuleType type,
            String condition,
            AdjustmentType adjustmentType,
            String adjustmentValue,
            int priority
    ) {
        return PricingRule.builder()
                .id(UUID.randomUUID())
                .name(type.name() + " test rule")
                .ruleType(type)
                .conditionValue(condition)
                .adjustmentType(adjustmentType)
                .adjustmentValue(new BigDecimal(adjustmentValue))
                .targetType(PricingRuleTarget.TICKET)
                .minTicketQty(0)
                .minComboQty(0)
                .priority(priority)
                .isActive(true)
                .build();
    }

    private PricingRule promotionRule(
            UUID promotionId,
            AdjustmentType adjustmentType,
            String adjustmentValue,
            PricingRuleTarget target,
            int minTickets,
            int minCombos,
            Integer priority
    ) {
        return promotionRuleWithCondition(
                promotionId.toString(), adjustmentType, adjustmentValue,
                target, minTickets, minCombos, priority
        );
    }

    private PricingRule promotionRuleWithCondition(
            String condition,
            AdjustmentType adjustmentType,
            String adjustmentValue,
            PricingRuleTarget target,
            int minTickets,
            int minCombos,
            Integer priority
    ) {
        return PricingRule.builder()
                .id(UUID.randomUUID())
                .name("Promotion rule")
                .ruleType(PricingRuleType.PROMOTION)
                .conditionValue(condition)
                .adjustmentType(adjustmentType)
                .adjustmentValue(new BigDecimal(adjustmentValue))
                .targetType(target)
                .minTicketQty(minTickets)
                .minComboQty(minCombos)
                .priority(priority)
                .isActive(true)
                .build();
    }

    private Promotion activePromotion(
            UUID id,
            String title,
            int startYear,
            int startMonth,
            int startDay,
            int endYear,
            int endMonth,
            int endDay
    ) {
        return Promotion.builder()
                .id(id)
                .title(title)
                .startDate(LocalDateTime.of(startYear, startMonth, startDay, 0, 0))
                .endDate(LocalDateTime.of(endYear, endMonth, endDay, 0, 0))
                .isActive(true)
                .build();
    }

    private PricingRuleRequest request(
            String name,
            PricingRuleType type,
            String condition,
            AdjustmentType adjustmentType,
            String adjustmentValue
    ) {
        PricingRuleRequest request = new PricingRuleRequest();
        request.setName(name);
        request.setRuleType(type);
        request.setConditionValue(condition);
        request.setAdjustmentType(adjustmentType);
        request.setAdjustmentValue(new BigDecimal(adjustmentValue));
        request.setTargetType(PricingRuleTarget.TICKET);
        request.setMinTicketQty(0);
        request.setMinComboQty(0);
        request.setPriority(1);
        request.setIsActive(true);
        return request;
    }

    private PricingRule identifiedRule(
            UUID id,
            String name,
            PricingRuleType type,
            String condition,
            AdjustmentType adjustmentType,
            String adjustmentValue,
            int priority
    ) {
        PricingRule rule = rule(type, condition, adjustmentType, adjustmentValue, priority);
        rule.setId(id);
        rule.setName(name);
        return rule;
    }

    private PricingRule identifiedPromotionRule(UUID id, UUID promotionId, int priority) {
        PricingRule rule = promotionRule(
                promotionId, AdjustmentType.PERCENTAGE,
                "10", PricingRuleTarget.TICKET, 0, 0, priority
        );
        rule.setId(id);
        return rule;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
