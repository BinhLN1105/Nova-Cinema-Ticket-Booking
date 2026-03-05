package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Mã nhập tay: VD "SUMMER30", "NEWUSER50K"
    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    // Nếu PERCENTAGE: 30.00 = giảm 30%. Nếu FIXED_AMOUNT: 50000 = giảm 50,000đ
    @Column(name = "discount_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountValue;

    // Số tiền giảm tối đa khi dùng PERCENTAGE (null = không giới hạn)
    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    // Giá trị đơn hàng tối thiểu để áp mã
    @Column(name = "min_order", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrder = BigDecimal.ZERO;

    // Tổng số lần dùng tối đa (null = không giới hạn)
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Business logic helper ──────────────────────────────────────────────

    /**
     * Tính số tiền được giảm thực tế dựa vào tổng đơn hàng.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (discountType == DiscountType.FIXED_AMOUNT) {
            return discountValue;
        }
        // PERCENTAGE
        BigDecimal discount = orderAmount
            .multiply(discountValue)
            .divide(BigDecimal.valueOf(100));
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            return maxDiscount;
        }
        return discount;
    }
}
