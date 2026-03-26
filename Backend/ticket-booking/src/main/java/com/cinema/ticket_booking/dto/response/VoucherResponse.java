package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
public class VoucherResponse implements Serializable {
    private static final long serialVersionUID = 1L;

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
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String description;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscount;
        private BigDecimal minOrder;
        private LocalDateTime validTo;
    }
}
