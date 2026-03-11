package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.ScreenType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScreenRequest {

    private String cinemaId;

    @NotBlank(message = "Tên phòng không được để trống")
    private String name;

    @NotNull(message = "Loại phòng không được để trống")
    private ScreenType screenType;

    @Min(value = 1, message = "Số hàng phải lớn hơn 0")
    private Integer totalRows;

    @Min(value = 1, message = "Số cột phải lớn hơn 0")
    private Integer totalCols;
}
