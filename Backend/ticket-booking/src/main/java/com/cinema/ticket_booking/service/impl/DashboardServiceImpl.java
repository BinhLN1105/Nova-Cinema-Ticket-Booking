package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RecentBooking;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RevenueByDay;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.TopMovie;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.TopMovie.MovieSlim;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(LocalDateTime startDate, LocalDateTime endDate, UUID cinemaId) {
        // Fix for empty UUID strings from frontend
        if (cinemaId != null && (cinemaId.toString().isEmpty()
                || cinemaId.toString().equals("00000000-0000-0000-0000-000000000000"))) {
            cinemaId = null;
        }

        log.info("📊 Fetching Stats: [{} TO {}] | Cinema: {}", startDate, endDate, cinemaId);

        // 0. Validation: Giới hạn 180 ngày để bảo vệ DB
        long daysInRange = Duration.between(startDate, endDate).toDays();
        if (daysInRange > 180) {
            throw new IllegalArgumentException("Khoảng thời gian thống kê không được vượt quá 180 ngày.");
        }

        try {
            // Tính toán doanh thu thuần
            BigDecimal netTotalRevenue = bookingRepository.calculateNetRevenue(startDate, endDate, cinemaId);
            BigDecimal totalDiscountGiven = bookingRepository.calculateTotalDiscounts(startDate, endDate, cinemaId);
            Long totalBookings = bookingRepository.countTotalBookingsByDateRange(startDate, endDate, cinemaId);

            // Phân tích doanh thu vé
            List<BookingRepository.RevenueBreakdownProjection> ticketData = bookingRepository
                    .getTicketRevenueBySeatType(startDate, endDate, cinemaId);
            BigDecimal totalTicketRev = BigDecimal.ZERO;
            java.util.Map<String, BigDecimal> ticketBreakdown = new java.util.HashMap<>();
            for (var row : ticketData) {
                totalTicketRev = totalTicketRev.add(row.getGrossRevenue());
                ticketBreakdown.put(row.getName(), row.getGrossRevenue());
            }
            DashboardStatsResponse.GrossTicketRevenue grossTicketRevenue = new DashboardStatsResponse.GrossTicketRevenue(
                    totalTicketRev, ticketBreakdown);

            // Phân tích doanh thu combo
            List<BookingRepository.RevenueBreakdownProjection> concessionData = bookingRepository
                    .getConcessionRevenueByCombo(startDate, endDate, cinemaId);
            BigDecimal totalConcessionRev = BigDecimal.ZERO;
            java.util.Map<String, BigDecimal> concessionBreakdown = new java.util.HashMap<>();
            for (var row : concessionData) {
                totalConcessionRev = totalConcessionRev.add(row.getGrossRevenue());
                concessionBreakdown.put(row.getName(), row.getGrossRevenue());
            }
            DashboardStatsResponse.GrossConcessionRevenue grossConcessionRevenue = new DashboardStatsResponse.GrossConcessionRevenue(
                    totalConcessionRev, concessionBreakdown);

            Long totalMovies = movieRepository.countByStatus(MovieStatus.NOW_SHOWING);
            Long totalUsers = userRepository.count();

            // Tính toán sự thay đổi so với kỳ trước
            double revenueChange = 0;
            double bookingChange = 0;

            LocalDateTime prevStartDate = startDate.minus(Duration.between(startDate, endDate));
            LocalDateTime prevEndDate = startDate;

            BigDecimal prevNetRevenue = bookingRepository.calculateNetRevenue(prevStartDate, prevEndDate, cinemaId);
            long prevBookings = bookingRepository.countTotalBookingsByDateRange(prevStartDate, prevEndDate, cinemaId);

            if (prevNetRevenue != null && prevNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenueChange = netTotalRevenue.subtract(prevNetRevenue)
                        .divide(prevNetRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100)).doubleValue();
            } else if (netTotalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenueChange = 100.0;
            }

            if (prevBookings > 0) {
                bookingChange = ((double) (totalBookings - prevBookings) / prevBookings) * 100;
            } else if (totalBookings > 0) {
                bookingChange = 100.0;
            }

            // Lấy dữ liệu doanh thu theo ngày
            List<BookingRepository.RevenueByDayProjection> dbRevData = bookingRepository
                    .getRevenueByDayInRange(startDate, endDate, cinemaId);
            List<BookingRepository.RevenueByDayProjection> dbTicketData = bookingRepository
                    .getDailyTicketRevenueInRange(startDate, endDate, cinemaId);
            List<BookingRepository.RevenueByDayProjection> dbConcessionData = bookingRepository
                    .getDailyConcessionRevenueInRange(startDate, endDate, cinemaId);

            List<RevenueByDay> revenueByDay = new ArrayList<>();
            long daysBetween = Duration.between(startDate, endDate).toDays();

            for (int i = 0; i < (int) daysBetween + 1; i++) {
                LocalDateTime currentDateTime = startDate.plusDays(i);
                String dateStr = currentDateTime.toLocalDate().format(DateTimeFormatter.ISO_DATE);

                BigDecimal totalRev = BigDecimal.ZERO;
                BigDecimal ticketRev = BigDecimal.ZERO;
                BigDecimal concessionRev = BigDecimal.ZERO;
                Long dayCount = 0L;

                // Total & Count
                for (BookingRepository.RevenueByDayProjection row : dbRevData) {
                    if (dateStr.equals(row.getDate().toString())) {
                        totalRev = row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO;
                        dayCount = row.getBookingCount() != null ? row.getBookingCount() : 0L;
                        break;
                    }
                }
                // Ticket Split
                for (BookingRepository.RevenueByDayProjection row : dbTicketData) {
                    if (row.getDate() != null && dateStr.equals(row.getDate().toString())) {
                        ticketRev = row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO;
                        break;
                    }
                }
                // Concession Split
                for (BookingRepository.RevenueByDayProjection row : dbConcessionData) {
                    if (row.getDate() != null && dateStr.equals(row.getDate().toString())) {
                        concessionRev = row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO;
                        break;
                    }
                }

                revenueByDay.add(new RevenueByDay(dateStr, totalRev, ticketRev, concessionRev, dayCount));
            }

            // Lấy top 5 phim
            List<BookingRepository.TopMovieProjection> topData = bookingRepository.getTop5MoviesInRange(startDate,
                    endDate, cinemaId);
            List<TopMovie> topMovies = new ArrayList<>();
            for (BookingRepository.TopMovieProjection row : topData) {
                MovieSlim movieSlim = new MovieSlim(row.getId(), row.getTitle(), row.getPosterUrl());
                topMovies.add(new TopMovie(movieSlim, row.getTickets(), row.getRev()));
            }

            // Lấy danh sách booking gần đây
            List<BookingRepository.RecentBookingProjection> recentData = bookingRepository
                    .getRecentBookingsInRange(startDate, endDate, cinemaId);
            List<RecentBooking> recentBookings = recentData.stream()
                    .map(row -> RecentBooking.builder()
                            .id(row.getId())
                            .bookingCode(row.getBookingCode())
                            .movieTitle(row.getMovieTitle())
                            .cinemaName(row.getCinemaName())
                            .startTime(row.getStartTime())
                            .totalAmount(row.getTotalAmount())
                            .status(row.getStatus())
                            .build())
                    .collect(Collectors.toList());

            return DashboardStatsResponse.builder()
                    .netTotalRevenue(netTotalRevenue)
                    .totalDiscountGiven(totalDiscountGiven)
                    .grossTicketRevenue(grossTicketRevenue)
                    .grossConcessionRevenue(grossConcessionRevenue)
                    .revenueChange(revenueChange)
                    .totalBookings(totalBookings)
                    .bookingChange(bookingChange)
                    .totalMovies(totalMovies)
                    .totalUsers(totalUsers)
                    .revenueByDay(revenueByDay)
                    .topMovies(topMovies)
                    .recentBookings(recentBookings)
                    .build();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi lấy dữ liệu Dashboard", e);
            throw new RuntimeException("Không thể lấy dữ liệu thống kê", e);
        }
    }
}
