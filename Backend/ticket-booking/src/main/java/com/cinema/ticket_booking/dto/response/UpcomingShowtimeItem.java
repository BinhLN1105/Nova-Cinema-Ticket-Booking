package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UpcomingShowtimeItem {

    private String showtimeId;
    private String movieTitle;
    private String moviePosterUrl;
    private String screenName;
    private LocalDateTime startTime;
    /** Số phút còn lại đến khi bắt đầu */
    private long minutesUntilStart;
    /** "SOON" nếu < 15 phút, "UPCOMING" nếu còn nhiều hơn */
    private String urgency;
}
