package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private List<OccupancyData> occupancyByCinema;
    private List<PeakHourData> peakHours;
    private List<RevenueByCinema> revenueByCinema;

    @Data
    @AllArgsConstructor
    public static class OccupancyData {
        private String cinemaName;
        private long totalSeats;
        private long bookedSeats;
        private double occupancyRate; // 0 - 100
    }

    @Data
    @AllArgsConstructor
    public static class PeakHourData {
        private int hour; // 0-23
        private long bookingCount;
    }

    @Data
    @AllArgsConstructor
    public static class RevenueByCinema {
        private String cinemaName;
        private BigDecimal revenue;
        private long bookingCount;
    }
}
