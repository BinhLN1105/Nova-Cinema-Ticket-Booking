package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.SeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScreenSeatLayoutRequest {

    @NotBlank(message = "ID phòng chiếu không được để trống")
    private String screenId;

    @Valid
    @NotNull(message = "Danh sách ghế không được null")
    private List<SeatDefinition> seats;

    @Data
    public static class SeatDefinition {
        @NotNull @Min(0)
        private Integer gridRow;

        @NotNull @Min(0)
        private Integer gridCol;

        @NotBlank(message = "Nhãn ghế không được để trống")
        private String seatLabel;

        @NotNull(message = "Loại ghế không được để trống")
        private SeatType seatType;
    }
}
