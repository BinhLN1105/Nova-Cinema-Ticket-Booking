package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Trả về sau khi nhân viên quét QR check-in.
 */
@Data
@Builder
public class CheckInResponse {

    private String bookingCode;
    private String movieTitle;
    private LocalDateTime startTime;
    private String cinemaName;
    private String screenName;

    // Danh sách ghế trong booking
    private List<SeatItem> seats;

    private Boolean allCheckedIn; // true nếu tất cả ghế đã được check-in
    private LocalDateTime checkedInAt;

    @Data
    @Builder
    public static class SeatItem {
        private Character rowLabel;
        private Integer colNumber;
        private String seatType;
        private Boolean isUsed;
    }
}
