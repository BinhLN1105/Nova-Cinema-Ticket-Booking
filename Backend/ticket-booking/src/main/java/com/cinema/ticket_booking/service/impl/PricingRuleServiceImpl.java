package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PricingRuleMapper;
import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.repository.PricingRuleRepository;
import com.cinema.ticket_booking.service.PricingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingRuleServiceImpl implements PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final PricingRuleMapper pricingRuleMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PricingRuleResponse> getAll(Pageable pageable) {
        Page<PricingRule> page = pricingRuleRepository.findAllByOrderByPriorityAsc(pageable);
        return PageResponse.of(page.map(pricingRuleMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRuleResponse getById(UUID id) {
        return pricingRuleMapper.toResponse(findById(id));
    }

    @Override
    public PricingRuleResponse create(PricingRuleRequest request) {
        PricingRule rule = pricingRuleMapper.toEntity(request);
        pricingRuleRepository.save(rule);
        return pricingRuleMapper.toResponse(rule);
    }

    @Override
    public PricingRuleResponse update(UUID id, PricingRuleRequest request) {
        PricingRule rule = findById(id);
        pricingRuleMapper.updateEntity(rule, request);
        pricingRuleRepository.save(rule);
        return pricingRuleMapper.toResponse(rule);
    }

    @Override
    public void delete(UUID id) {
        pricingRuleRepository.deleteById(id);
    }

    @Override
    public PricingRuleResponse toggleActive(UUID id) {
        PricingRule rule = findById(id);
        rule.setIsActive(!rule.getIsActive());
        pricingRuleRepository.save(rule);
        return pricingRuleMapper.toResponse(rule);
    }

    private PricingRule findById(UUID id) {
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quy tắc giá", id));
    }
}
