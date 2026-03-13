package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PricingRuleService {
    PageResponse<PricingRuleResponse> getAll(Pageable pageable);
    PricingRuleResponse getById(UUID id);
    PricingRuleResponse create(PricingRuleRequest request);
    PricingRuleResponse update(UUID id, PricingRuleRequest request);
    void delete(UUID id);
    PricingRuleResponse toggleActive(UUID id);
}
