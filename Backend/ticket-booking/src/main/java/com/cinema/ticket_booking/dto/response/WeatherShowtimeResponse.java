package com.cinema.ticket_booking.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherShowtimeResponse {
    private String condition;
    private Double temperature;
    private boolean isBadWeather;
    private boolean outOfForecastRange;
    private String warningMessage;
}
