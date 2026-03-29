package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.AdjustmentType;
import com.cinema.ticket_booking.enums.PricingRuleTarget;
import com.cinema.ticket_booking.enums.PricingRuleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private PricingRuleType ruleType;

    // Ví dụ: "TUESDAY", "22:00-23:59", "2024-04-30,2024-05-01", "VIP"
    @Column(name = "condition_value", nullable = false)
    private String conditionValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    // Giá trị điều chỉnh (vd: -10 cho giảm 10%, 1.5 cho x1.5 giá, 20000 cho +20k)
    @Column(name = "adjustment_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal adjustmentValue;

    // Mục tiêu áp dụng: TICKET, COMBO, hoặc ORDER_TOTAL
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    @Builder.Default
    private PricingRuleTarget targetType = PricingRuleTarget.TICKET;

    // Số lượng vé tối thiểu để áp dụng rule này (hỗ trợ Bundle)
    @Column(name = "min_ticket_qty", nullable = false)
    @Builder.Default
    private Integer minTicketQty = 0;

    // Số lượng Combo tối thiểu để áp dụng rule này (hỗ trợ Bundle)
    @Column(name = "min_combo_qty", nullable = false)
    @Builder.Default
    private Integer minComboQty = 0;

    // Độ ưu tiên xử lý, số nhỏ xử lý trước. (Để áp dụng % trước/sau cộng tiền)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
