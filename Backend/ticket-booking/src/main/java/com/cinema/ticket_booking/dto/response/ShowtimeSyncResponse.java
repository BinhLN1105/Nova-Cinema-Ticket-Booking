package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSyncResponse {
    private UUID id;
    private UUID movieId;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
