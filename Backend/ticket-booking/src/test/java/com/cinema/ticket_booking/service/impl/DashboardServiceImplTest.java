package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
    void testGetStats_IntervalExceeded() {
        LocalDateTime start = LocalDateTime.now().minusDays(185);
        LocalDateTime end = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> 
            dashboardService.getStats(start, end, null)
        );
    }

    @Test
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

        // Mock Daily breakdown projections
        BookingRepository.RevenueByDayProjection dayRevProj = mock(BookingRepository.RevenueByDayProjection.class);
        when(dayRevProj.getDate()).thenReturn(startDate.toLocalDate().toString());
        when(dayRevProj.getRevenue()).thenReturn(new BigDecimal("100000"));
        when(dayRevProj.getBookingCount()).thenReturn(2L);
        when(bookingRepository.getRevenueByDayInRange(any(), any(), eq(cinemaId))).thenReturn(List.of(dayRevProj));

        when(bookingRepository.getDailyTicketRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());
        when(bookingRepository.getDailyConcessionRevenueInRange(any(), any(), eq(cinemaId))).thenReturn(Collections.emptyList());

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
    void testGetStats_PrevRevenueAndBookingsZero() {
        UUID cinemaId = UUID.randomUUID();

        // 1st call for current, 2nd call for prev (which returns null/0)
        when(bookingRepository.calculateNetRevenue(any(), any(), eq(cinemaId)))
                .thenReturn(new BigDecimal("1000000"))
                .thenReturn(BigDecimal.ZERO);
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
    void testGetStats_DatabaseException() {
        when(bookingRepository.calculateNetRevenue(any(), any(), any())).thenThrow(new RuntimeException("DB Connection down"));

        assertThrows(RuntimeException.class, () ->
            dashboardService.getStats(startDate, endDate, null)
        );
    }
}
