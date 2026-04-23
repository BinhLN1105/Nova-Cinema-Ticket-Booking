package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CheckInHistoryItemResponse {

    private String bookingCode;       // "BK20241201001" — null nếu QR invalid
    private String customerName;
    private String customerPhone;
    private String movieTitle;
    private String moviePosterUrl;
    private String screenName;
    private String cinemaName;
    private String seatsChecked;      // "A3, A4, B5"
    private boolean success;
    private String failReason;        // null nếu thành công
    private LocalDateTime scannedAt;
}
