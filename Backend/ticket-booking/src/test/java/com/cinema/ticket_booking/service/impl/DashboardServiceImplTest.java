package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(7);
        endDate = LocalDateTime.now();
    }

    @Test
    @DisplayName("1. Vượt quá 180 ngày ném IllegalArgumentException")
    void testGetStats_IntervalExceeded() {
        LocalDateTime start = LocalDateTime.now().minusDays(185);
        LocalDateTime end = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> 
            dashboardService.getStats(start, end, null)
        );
    }

    @Test
    @DisplayName("2. UUID rỗng hoặc 00000000... được chuyển đổi thành null cinemaId")
    void testGetStats_EmptyCinemaIdConvertedToNull() {
        UUID emptyId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        when(bookingRepository.calculateNetRevenue(any(), any(), eq(null))).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.calculateTotalDiscounts(any(), any(), eq(null))).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.countTotalBookingsByDateRange(any(), any(), eq(null))).thenReturn(0L);

        when(bookingRepository.getTicketRevenueBySeatType(any(), any(), eq(null))).thenReturn(Collections.emptyList());
        when(bookingRepository.getConcessionRevenueByCombo(any(), any(), eq(null))).thenReturn(Collections.emptyList());

        when(movieRepository.countByStatus(MovieStatus.NOW_SHOWING)).thenReturn(10L);
        when(userRepository.count()).thenReturn(100L);

        when(bookingRepository.getRevenueByDayInRange(any(), any(), eq(null))).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyTicketRevenueInRange(any(), any(), eq(null))).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyConcessionRevenueInRange(any(), any(), eq(null))).thenReturn(Collections.emptyList());

        when(bookingRepository.getTop5MoviesInRange(any(), any(), eq(null))).thenReturn(Collections.emptyList());
        when(bookingRepository.getRecentBookingsInRange(any(), any(), eq(null))).thenReturn(Collections.emptyList());

        DashboardStatsResponse stats = dashboardService.getStats(startDate, endDate, emptyId);
        assertNotNull(stats);
        assertEquals(0L, stats.getTotalBookings());
    }

    @Test
    @DisplayName("3. Lấy thống kê đầy đủ thành công bao gồm phân bổ vé & combo theo ngày")
    void testGetStats_Success() {
        UUID cinemaId = UUID.randomUUID();

        when(bookingRepository.calculateNetRevenue(any(), any(), eq(cinemaId)))
                .thenReturn(new BigDecimal("1000000"))
                .thenReturn(new BigDecimal("800000"));
        when(bookingRepository.calculateTotalDiscounts(any(), any(), eq(cinemaId))).thenReturn(new BigDecimal("50000"));
        when(bookingRepository.countTotalBookingsByDateRange(any(), any(), eq(cinemaId)))
                .thenReturn(15L)
                .thenReturn(10L);

        // Mock Ticket projection
        BookingRepository.RevenueBreakdownProjection ticketProj = mock(BookingRepository.RevenueBreakdownProjection.class);
        when(ticketProj.getName()).thenReturn("VIP");
        when(ticketProj.getGrossRevenue()).thenReturn(new BigDecimal("600000"));
        when(bookingRepository.getTicketRevenueBySeatType(any(), any(), eq(cinemaId))).thenReturn(List.of(ticketProj));

        // Mock Concession projection
        BookingRepository.RevenueBreakdownProjection concessionProj = mock(BookingRepository.RevenueBreakdownProjection.class);
        when(concessionProj.getName()).thenReturn("Corn Combo");
        when(concessionProj.getGrossRevenue()).thenReturn(new BigDecimal("400000"));
        when(bookingRepository.getConcessionRevenueByCombo(any(), any(), eq(cinemaId))).thenReturn(List.of(concessionProj));

        when(movieRepository.countByStatus(MovieStatus.NOW_SHOWING)).thenReturn(10L);
        when(userRepository.count()).thenReturn(100L);

        String targetDateStr = startDate.toLocalDate().toString();

        // Mock Daily breakdown projections
        BookingRepository.RevenueByDayProjection dayRevProj = mock(BookingRepository.RevenueByDayProjection.class);
        when(dayRevProj.getDate()).thenReturn(targetDateStr);
        when(dayRevProj.getRevenue()).thenReturn(new BigDecimal("100000"));
        when(dayRevProj.getBookingCount()).thenReturn(2L);
        when(bookingRepository.getRevenueByDayInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(dayRevProj));

        BookingRepository.RevenueByDayProjection dailyTicketProj = mock(BookingRepository.RevenueByDayProjection.class);
        when(dailyTicketProj.getDate()).thenReturn(targetDateStr);
        when(dailyTicketProj.getRevenue()).thenReturn(new BigDecimal("60000"));
        when(bookingRepository.getDailyTicketRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(dailyTicketProj));

        BookingRepository.RevenueByDayProjection dailyConcessionProj = mock(BookingRepository.RevenueByDayProjection.class);
        when(dailyConcessionProj.getDate()).thenReturn(targetDateStr);
        when(dailyConcessionProj.getRevenue()).thenReturn(new BigDecimal("40000"));
        when(bookingRepository.getDailyConcessionRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(dailyConcessionProj));

        // Mock Top Movies
        BookingRepository.TopMovieProjection topMovieProj = mock(BookingRepository.TopMovieProjection.class);
        when(topMovieProj.getId()).thenReturn(UUID.randomUUID().toString());
        when(topMovieProj.getTitle()).thenReturn("Avengers");
        when(topMovieProj.getPosterUrl()).thenReturn("avengers.jpg");
        when(topMovieProj.getTickets()).thenReturn(50L);
        when(topMovieProj.getRev()).thenReturn(new BigDecimal("500000"));
        when(bookingRepository.getTop5MoviesInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(topMovieProj));

        // Mock Recent bookings
        BookingRepository.RecentBookingProjection recentProj = mock(BookingRepository.RecentBookingProjection.class);
        when(recentProj.getId()).thenReturn(UUID.randomUUID().toString());
        when(recentProj.getBookingCode()).thenReturn("B001");
        when(recentProj.getMovieTitle()).thenReturn("Avengers");
        when(recentProj.getCinemaName()).thenReturn("Cinema 1");
        when(recentProj.getStartTime()).thenReturn(LocalDateTime.now());
        when(recentProj.getTotalAmount()).thenReturn(new BigDecimal("150000"));
        when(recentProj.getStatus()).thenReturn("SUCCESS");
        when(bookingRepository.getRecentBookingsInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(recentProj));

        DashboardStatsResponse stats = dashboardService.getStats(startDate, endDate, cinemaId);

        assertNotNull(stats);
        assertEquals(new BigDecimal("1000000"), stats.getNetTotalRevenue());
        assertEquals(new BigDecimal("50000"), stats.getTotalDiscountGiven());
        assertEquals(15L, stats.getTotalBookings());
        assertEquals(25.0, stats.getRevenueChange()); // (1000000 - 800000) / 800000 * 100
        assertEquals(50.0, stats.getBookingChange()); // (15 - 10) / 10 * 100
        assertEquals(10L, stats.getTotalMovies());
        assertEquals(100L, stats.getTotalUsers());
        assertFalse(stats.getRevenueByDay().isEmpty());
        assertEquals(1, stats.getTopMovies().size());
        assertEquals(1, stats.getRecentBookings().size());
    }

    @Test
    @DisplayName("4. Doanh thu kỳ trước bằng 0 hoặc null, trả về change = 100%")
    void testGetStats_PrevRevenueAndBookingsZero() {
        UUID cinemaId = UUID.randomUUID();

        when(bookingRepository.calculateNetRevenue(any(), any(), eq(cinemaId)))
                .thenReturn(new BigDecimal("1000000"))
                .thenReturn(null);
        when(bookingRepository.calculateTotalDiscounts(any(), any(), eq(cinemaId))).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.countTotalBookingsByDateRange(any(), any(), eq(cinemaId)))
                .thenReturn(15L)
                .thenReturn(0L);

        when(bookingRepository.getTicketRevenueBySeatType(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());
        when(bookingRepository.getConcessionRevenueByCombo(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());

        when(movieRepository.countByStatus(MovieStatus.NOW_SHOWING)).thenReturn(10L);
        when(userRepository.count()).thenReturn(100L);

        when(bookingRepository.getRevenueByDayInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyTicketRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyConcessionRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());

        when(bookingRepository.getTop5MoviesInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());
        when(bookingRepository.getRecentBookingsInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());

        DashboardStatsResponse stats = dashboardService.getStats(startDate, endDate, cinemaId);

        assertNotNull(stats);
        assertEquals(100.0, stats.getRevenueChange());
        assertEquals(100.0, stats.getBookingChange());
    }

    @Test
    @DisplayName("5. Cả kỳ này và kỳ trước đều không có doanh thu/booking, change = 0%")
    void testGetStats_ZeroBothPeriods() {
        when(bookingRepository.calculateNetRevenue(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO);
        when(bookingRepository.calculateTotalDiscounts(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.countTotalBookingsByDateRange(any(), any(), any()))
                .thenReturn(0L)
                .thenReturn(0L);

        when(bookingRepository.getTicketRevenueBySeatType(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.getConcessionRevenueByCombo(any(), any(), any())).thenReturn(Collections.emptyList());

        when(movieRepository.countByStatus(MovieStatus.NOW_SHOWING)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);

        when(bookingRepository.getRevenueByDayInRange(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyTicketRevenueInRange(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyConcessionRevenueInRange(any(), any(), any())).thenReturn(Collections.emptyList());

        when(bookingRepository.getTop5MoviesInRange(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.getRecentBookingsInRange(any(), any(), any())).thenReturn(Collections.emptyList());

        DashboardStatsResponse stats = dashboardService.getStats(startDate, endDate, null);

        assertNotNull(stats);
        assertEquals(0.0, stats.getRevenueChange());
        assertEquals(0.0, stats.getBookingChange());
    }

    @Test
    @DisplayName("6. Xử lý lỗi Database exception và ném RuntimeException")
    void testGetStats_DatabaseException() {
        when(bookingRepository.calculateNetRevenue(any(), any(), any())).thenThrow(new RuntimeException("DB Connection down"));

        assertThrows(RuntimeException.class, () ->
            dashboardService.getStats(startDate, endDate, null)
        );
    }
}
