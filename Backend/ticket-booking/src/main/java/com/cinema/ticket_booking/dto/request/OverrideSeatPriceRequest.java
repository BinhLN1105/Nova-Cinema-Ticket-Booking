package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class OverrideSeatPriceRequest {
    
    @NotEmpty(message = "Danh sách ghế không được để trống")
    private List<UUID> showtimeSeatIds;

    @NotNull(message = "Giá mới không được để trống")
    private BigDecimal newPrice;
}
