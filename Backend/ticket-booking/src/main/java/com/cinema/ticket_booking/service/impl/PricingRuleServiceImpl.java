package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import com.cinema.ticket_booking.enums.PricingRuleType;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PricingRuleMapper;
import com.cinema.ticket_booking.model.PricingRule;
import com.cinema.ticket_booking.model.Promotion;
import com.cinema.ticket_booking.repository.PricingRuleRepository;
import com.cinema.ticket_booking.repository.PromotionRepository;
import com.cinema.ticket_booking.service.PricingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingRuleServiceImpl implements PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final PromotionRepository promotionRepository;
    private final PricingRuleMapper pricingRuleMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PricingRuleResponse> getAll(Pageable pageable) {
        Page<PricingRule> page = pricingRuleRepository.findAllByOrderByPriorityAsc(pageable);

        // --- CHỐNG LỖI N+1 KHI QUERY ---
        List<PricingRule> rules = page.getContent();

        Set<UUID> promoIds = rules.stream()
                .filter(r -> r.getRuleType() == PricingRuleType.PROMOTION)
                .map(r -> {
                    try {
                        return UUID.fromString(r.getConditionValue());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Promotion> promoMap = promoIds.isEmpty() ? Collections.emptyMap()
                : promotionRepository.findAllById(promoIds).stream()
                        .collect(Collectors.toMap(Promotion::getId, p -> p));

        List<PricingRuleResponse> responses = rules.stream()
                .map(rule -> mapToDetailedResponse(rule, promoMap))
                .toList();

        return PageResponse.of(new PageImpl<>(responses, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRuleResponse getById(UUID id) {
        PricingRule rule = findById(id);
        Map<UUID, Promotion> promoMap = Collections.emptyMap();
        if (rule.getRuleType() == PricingRuleType.PROMOTION) {
            try {
                UUID pId = UUID.fromString(rule.getConditionValue());
                Promotion p = promotionRepository.findById(pId).orElse(null);
                if (p != null)
                    promoMap = Map.of(p.getId(), p);
            } catch (Exception e) {
            }
        }
        return mapToDetailedResponse(rule, promoMap);
    }

    @Override
    public PricingRuleResponse create(PricingRuleRequest request) {
        PricingRule rule = pricingRuleMapper.toEntity(request);
        pricingRuleRepository.save(rule);
        return mapToDetailedResponse(rule, Collections.emptyMap());
    }

    @Override
    public PricingRuleResponse update(UUID id, PricingRuleRequest request) {
        PricingRule rule = findById(id);
        pricingRuleMapper.updateEntity(rule, request);
        pricingRuleRepository.save(rule);
        return mapToDetailedResponse(rule, Collections.emptyMap());
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
        return mapToDetailedResponse(rule, Collections.emptyMap());
    }

    private PricingRule findById(UUID id) {
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quy tắc giá", id));
    }

    private PricingRuleResponse mapToDetailedResponse(PricingRule rule, Map<UUID, Promotion> promoMap) {
        PricingRuleResponse resp = pricingRuleMapper.toResponse(rule);

        switch (rule.getRuleType()) {
            case PROMOTION:
                try {
                    UUID pId = UUID.fromString(rule.getConditionValue());
                    Promotion p = promoMap.get(pId);
                    if (p != null) {
                        resp.setPromotionTitle(p.getTitle());
                        resp.setStartDate(p.getStartDate());
                        resp.setEndDate(p.getEndDate());
                        resp.setConditionDisplay("Từ " +
                                p.getStartDate().toLocalDate() + " đến " + p.getEndDate().toLocalDate());
                    } else {
                        resp.setConditionDisplay("KM (ID:" + rule.getConditionValue().substring(0, 8) + "...)");
                    }
                } catch (Exception e) {
                    resp.setConditionDisplay("KM (ID không lệ)");
                }
                break;
            case DAY_OF_WEEK:
                resp.setConditionDisplay("Ngày trong tuần: " + rule.getConditionValue());
                break;
            case TIME_FRAME:
                resp.setConditionDisplay("Khung giờ: " + rule.getConditionValue());
                break;
            case SEAT_TYPE:
                resp.setConditionDisplay("Loại ghế: " + rule.getConditionValue());
                break;
            case DATE_RANGE:
                String[] dr = rule.getConditionValue().split(",");
                if (dr.length == 2) {
                    resp.setConditionDisplay("Từ " + dr[0] + " đến " + dr[1]);
                } else {
                    resp.setConditionDisplay(rule.getConditionValue());
                }
                break;
            default:
                resp.setConditionDisplay(rule.getConditionValue());
        }

        return resp;
    }
}
