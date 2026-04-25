package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpRequest {
    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 10000, message = "Số tiền nạp tối thiểu là 10.000 VNĐ")
    private BigDecimal amount;
}
