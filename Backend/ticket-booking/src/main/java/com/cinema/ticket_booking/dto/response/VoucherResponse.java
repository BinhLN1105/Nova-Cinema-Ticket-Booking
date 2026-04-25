package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.DiscountType;
import com.cinema.ticket_booking.enums.VoucherApplicableTo;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VoucherApplicableTo applicableTo;
    private Boolean isActive;
    private UserVoucherStatus status; // Add status field

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String description;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscount;
        private BigDecimal minOrder;
        private LocalDateTime endDate;
        private UserVoucherStatus status; // Add status field
    }
}
