package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import com.cinema.ticket_booking.model.PricingRule;
import org.springframework.stereotype.Component;

@Component
public class PricingRuleMapper {

    public PricingRule toEntity(PricingRuleRequest request) {
        if (request == null) {
            return null;
        }

        return PricingRule.builder()
                .name(request.getName())
                .ruleType(request.getRuleType())
                .conditionValue(request.getConditionValue())
                .adjustmentType(request.getAdjustmentType())
                .adjustmentValue(request.getAdjustmentValue())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    public PricingRuleResponse toResponse(PricingRule entity) {
        if (entity == null) {
            return null;
        }

        return PricingRuleResponse.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .ruleType(entity.getRuleType())
                .conditionValue(entity.getConditionValue())
                .adjustmentType(entity.getAdjustmentType())
                .adjustmentValue(entity.getAdjustmentValue())
                .priority(entity.getPriority())
                .isActive(entity.getIsActive())
                .build();
    }

    public void updateEntity(PricingRule entity, PricingRuleRequest request) {
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getRuleType() != null) entity.setRuleType(request.getRuleType());
        if (request.getConditionValue() != null) entity.setConditionValue(request.getConditionValue());
        if (request.getAdjustmentType() != null) entity.setAdjustmentType(request.getAdjustmentType());
        if (request.getAdjustmentValue() != null) entity.setAdjustmentValue(request.getAdjustmentValue());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
    }
}
