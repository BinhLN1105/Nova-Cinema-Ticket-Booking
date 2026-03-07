package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50)
    private String code;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    // Chỉ dùng khi discountType = PERCENTAGE
    private BigDecimal maxDiscount;

    @DecimalMin(value = "0.0", message = "Đơn tối thiểu không được âm")
    private BigDecimal minOrder;

    // null = không giới hạn lượt dùng
    @Min(value = 1, message = "Số lượt dùng phải lớn hơn 0")
    private Integer usageLimit;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime validTo;
}
