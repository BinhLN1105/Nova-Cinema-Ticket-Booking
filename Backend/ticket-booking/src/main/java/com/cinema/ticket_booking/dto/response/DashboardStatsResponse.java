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
import java.util.Map;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal netTotalRevenue;
    private BigDecimal totalDiscountGiven;
    private GrossTicketRevenue grossTicketRevenue;
    private GrossConcessionRevenue grossConcessionRevenue;
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
    @NoArgsConstructor
    public static class RevenueByDay implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date; // "YYYY-MM-DD" or "YYYY-MM"
        private BigDecimal revenue; // Total
        private BigDecimal ticketRevenue;
        private BigDecimal concessionRevenue;
        private Long bookingCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopMovie implements Serializable {
        private static final long serialVersionUID = 1L;
        private MovieSlim movie;
        private long bookings;
        private BigDecimal revenue;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class MovieSlim implements Serializable {
            private static final long serialVersionUID = 1L;
            private String id;
            private String title;
            private String posterUrl;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RecentBooking implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String bookingCode;
        private String movieTitle;
        private String cinemaName;
        private LocalDateTime startTime;
        private BigDecimal totalAmount;
        private String status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GrossTicketRevenue implements Serializable {
        private static final long serialVersionUID = 1L;
        private BigDecimal total;
        private Map<String, BigDecimal> breakdown;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GrossConcessionRevenue implements Serializable {
        private static final long serialVersionUID = 1L;
        private BigDecimal total;
        private Map<String, BigDecimal> breakdown;
    }
}
