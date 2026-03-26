package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleType;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private PricingRuleType ruleType;
    private String conditionValue;
    private AdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private Integer priority;
    private Boolean isActive;
}
