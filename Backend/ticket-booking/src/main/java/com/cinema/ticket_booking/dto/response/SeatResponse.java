package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.enums.SeatType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatResponse {

    private String id;

    // Vị trí ghế: "A1", "B5", ...
    private Character rowLabel;
    private Integer colNumber;
    private SeatType seatType;

    // Chỉ có khi query kết hợp với ShowtimeSeat
    private SeatStatus status;
    private BigDecimal price;

    // ID của ShowtimeSeat (dùng khi gửi request đặt ghế)
    private String showtimeSeatId;
}
