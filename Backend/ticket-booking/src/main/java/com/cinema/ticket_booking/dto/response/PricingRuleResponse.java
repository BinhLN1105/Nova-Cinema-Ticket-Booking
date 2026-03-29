package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleTarget;
import com.cinema.ticket_booking.enums.PricingRuleType;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String conditionDisplay; // Chuỗi hiển thị thân thiện (Ví dụ: Thứ 2, 08:00-10:00)
    private String promotionTitle; // Tên chương trình (Nếu là PROMOTION)
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private AdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private PricingRuleTarget targetType;
    private Integer minTicketQty;
    private Integer minComboQty;
    private Integer priority;
    private Boolean isActive;

}
