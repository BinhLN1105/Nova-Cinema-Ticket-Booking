package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.enums.SeatType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trả về toàn bộ sơ đồ ghế của 1 suất chiếu.
 * Android dùng để render màn hình chọn ghế (seat picker).
 */
@Data
@Builder
public class SeatMapResponse {

    private String showtimeId;
    private Integer totalRows;
    private Integer totalCols;

    // Danh sách tất cả ghế trong phòng với trạng thái hiện tại
    private List<SeatItem> seats;

    @Data
    @Builder
    public static class SeatItem {
        private String showtimeSeatId; // ID để gửi lên khi đặt
        private String seatId;
        private Character rowLabel;
        private Integer colNumber;
        private SeatType seatType;
        private SeatStatus status; // AVAILABLE | LOCKED | BOOKED
        private BigDecimal price;
    }
}
