package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateComboRequest {

    @NotBlank(message = "Tên combo không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá combo phải lớn hơn 0")
    private BigDecimal price;

    private String type;
    
    private Boolean isAvailable;
}
