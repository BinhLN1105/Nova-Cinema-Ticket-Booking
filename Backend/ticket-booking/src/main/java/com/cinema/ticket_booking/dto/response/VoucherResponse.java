package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherResponse {

    private String id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrder;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;

    // Tóm tắt hiển thị cho user (không lộ usedCount, usageLimit)
    @Data
    @Builder
    public static class Summary {
        private String code;
        private String description;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscount;
        private BigDecimal minOrder;
        private LocalDateTime validTo;
    }
}
