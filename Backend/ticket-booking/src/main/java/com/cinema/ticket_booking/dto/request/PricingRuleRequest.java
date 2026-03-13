package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingRuleRequest {
    @NotBlank(message = "Tên quy tắc không được để trống")
    private String name;

    @NotNull(message = "Loại quy tắc không được để trống")
    private PricingRuleType ruleType;

    @NotBlank(message = "Giá trị điều kiện không được để trống")
    private String conditionValue;

    @NotNull(message = "Loại điều chỉnh không được để trống")
    private AdjustmentType adjustmentType;

    @NotNull(message = "Giá trị điều chỉnh không được để trống")
    private BigDecimal adjustmentValue;

    private Integer priority = 0;
    private Boolean isActive = true;
}
