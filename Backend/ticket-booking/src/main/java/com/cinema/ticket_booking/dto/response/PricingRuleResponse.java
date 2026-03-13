package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PricingRuleResponse {
    private String id;
    private String name;
    private PricingRuleType ruleType;
    private String conditionValue;
    private AdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private Integer priority;
    private Boolean isActive;
}
