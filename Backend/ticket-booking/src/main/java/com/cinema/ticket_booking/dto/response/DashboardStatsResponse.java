package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private BigDecimal totalRevenue;
    private double revenueChange; // phần trăm thay đổi so với tháng trước
    private long totalBookings;
    private double bookingChange; // phần trăm thay đổi so với tháng trước
    private long totalMovies; // đang chiếu
    private long totalUsers;

    private List<RevenueByDay> revenueByDay;
    private List<TopMovie> topMovies;
    private List<RecentBooking> recentBookings;

    @Data
    @AllArgsConstructor
    public static class RevenueByDay {
        private String date; // "YYYY-MM-DD"
        private BigDecimal revenue;
    }

    @Data
    @AllArgsConstructor
    public static class TopMovie {
        private MovieSlim movie;
        private long bookings;
        private BigDecimal revenue;

        @Data
        @AllArgsConstructor
        public static class MovieSlim {
            private String id;
            private String title;
            private String posterUrl;
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class RecentBooking {
        private String id;
        private String bookingCode;
        private String movieTitle;
        private String cinemaName;
        private LocalDateTime startTime;
        private BigDecimal totalAmount;
        private String status;
    }
}
