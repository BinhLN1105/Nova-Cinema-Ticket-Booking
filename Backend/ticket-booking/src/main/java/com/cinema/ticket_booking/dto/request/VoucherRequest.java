package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.DiscountType;
import com.cinema.ticket_booking.enums.VoucherApplicableTo;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private BigDecimal maxDiscount;

    @DecimalMin(value = "0.0", message = "Đơn tối thiểu không được âm")
    private BigDecimal minOrder;

    @Min(value = 1, message = "Số lượt dùng phải lớn hơn 0")
    private Integer usageLimit;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Đối tượng áp dụng không được để trống")
    private VoucherApplicableTo applicableTo;
}
