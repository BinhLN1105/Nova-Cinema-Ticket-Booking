package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RecentBooking;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RevenueByDay;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.TopMovie;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.TopMovie.MovieSlim;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        log.info("Lấy dữ liệu thống kê Dashboard bằng Projections");
        try {
            // 1. Total statistics
            BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue();
            Long totalBookings = bookingRepository.countTotalBookings();
            
            // FIXME: create native query for MovieRepository and UserRepository counts if missing, 
            // but those repositories probably already extend JpaRepository. 
            // We use simple count() assuming we want all users, and for movies we need a custom method.
            Long totalMovies = movieRepository.countByStatus(com.cinema.ticket_booking.enums.MovieStatus.NOW_SHOWING);
            Long totalUsers = userRepository.count();

            double revenueChange = 12.5; 
            double bookingChange = 8.4;

            // 2. Revenue by day (last 7 days)
            List<BookingRepository.RevenueByDayProjection> revData = bookingRepository.getRevenueByDay();
            List<RevenueByDay> revenueByDay = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                BigDecimal dayRev = BigDecimal.ZERO;
                for (BookingRepository.RevenueByDayProjection row : revData) {
                    if (row.getDate() != null && row.getDate().equals(d.toString())) {
                        dayRev = row.getRevenue();
                        break;
                    }
                }
                revenueByDay.add(new RevenueByDay(d.format(DateTimeFormatter.ISO_DATE), dayRev));
            }

            // 3. Top Movies
            List<BookingRepository.TopMovieProjection> topData = bookingRepository.getTop5Movies();
            List<TopMovie> topMovies = new ArrayList<>();
            for (BookingRepository.TopMovieProjection row : topData) {
                MovieSlim movieSlim = new MovieSlim(row.getId(), row.getTitle(), row.getPosterUrl());
                topMovies.add(new TopMovie(movieSlim, row.getTickets(), row.getRev()));
            }

            // 4. Recent Bookings
            List<BookingRepository.RecentBookingProjection> recentData = bookingRepository.getRecentBookings();
            List<RecentBooking> recentBookings = new ArrayList<>();
            for (BookingRepository.RecentBookingProjection row : recentData) {
                RecentBooking rb = RecentBooking.builder()
                        .id(row.getId())
                        .bookingCode(row.getBookingCode())
                        .movieTitle(row.getMovieTitle())
                        .cinemaName(row.getCinemaName())
                        .startTime(row.getStartTime() != null ? row.getStartTime().toLocalDateTime() : null)
                        .totalAmount(row.getTotalAmount())
                        .status(row.getStatus())
                        .build();
                recentBookings.add(rb);
            }

            return DashboardStatsResponse.builder()
                    .totalRevenue(totalRevenue)
                    .revenueChange(revenueChange)
                    .totalBookings(totalBookings)
                    .bookingChange(bookingChange)
                    .totalMovies(totalMovies)
                    .totalUsers(totalUsers)
                    .revenueByDay(revenueByDay)
                    .topMovies(topMovies)
                    .recentBookings(recentBookings)
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi lấy dữ liệu Dashboard", e);
            throw new RuntimeException("Không thể lấy dữ liệu thống kê", e);
        }
    }
}
