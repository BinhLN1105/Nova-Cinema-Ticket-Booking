package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeRequest {

    @NotNull(message = "Phim không được để trống")
    private String movieId;

    @NotNull(message = "Phòng chiếu không được để trống")
    private String screenId;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn 0")
    private BigDecimal basePrice;
}
