package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.*;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffDashboardServiceImplTest {

    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private StaffDashboardServiceImpl staffDashboardService;

    @Test
    void testGetDashboardStats_Success() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).build();
        StaffProfile staffProfile = StaffProfile.builder().cinema(cinema).build();

        when(staffProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(staffProfile));
        when(showtimeRepository.countByCinemaAndDate(eq(cinema.getId()), any(LocalDate.class))).thenReturn(5L);
        when(ticketRepository.countCheckedInByCinemaAndDateRange(eq(cinema.getId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);

        StaffDashboardResponse stats = staffDashboardService.getDashboardStats(user);

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalShowtimesToday());
        assertEquals(10L, stats.getTicketsCheckedToday());
        assertEquals(10L, stats.getTicketsCheckedThisMonth());
    }

    @Test
    void testGetUpcomingShowtimes_Success() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).build();
        StaffProfile staffProfile = StaffProfile.builder().cinema(cinema).build();

        Movie movie = Movie.builder().title("Movie 1").posterUrl("poster-1").build();
        Screen screen = Screen.builder().name("Screen A").build();
        Showtime showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(staffProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(staffProfile));
        when(showtimeRepository.findUpcomingByCinema(eq(cinema.getId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(showtime));

        List<UpcomingShowtimeItem> items = staffDashboardService.getUpcomingShowtimes(user);

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Movie 1", items.get(0).getMovieTitle());
        assertEquals("SOON", items.get(0).getUrgency());
    }

    @Test
    void testGetCheckInHistory_Today() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).name("Cinema A").build();
        StaffProfile staffProfile = StaffProfile.builder().cinema(cinema).build();

        User customer = User.builder().fullName("Customer A").phone("0123").build();
        Movie movie = Movie.builder().title("Movie A").posterUrl("poster-A").build();
        Screen screen = Screen.builder().name("Screen A").build();
        Showtime showtime = Showtime.builder().movie(movie).screen(screen).build();

        Seat seat = Seat.builder().rowLabel('C').colNumber(5).build();
        ShowtimeSeat showtimeSeat = ShowtimeSeat.builder().seat(seat).build();
        BookingItem bookingItem = BookingItem.builder().showtimeSeat(showtimeSeat).build();

        Booking booking = Booking.builder()
                .bookingCode("CODE123")
                .user(customer)
                .cinema(cinema)
                .showtime(showtime)
                .createdAt(LocalDateTime.now())
                .bookingItems(List.of(bookingItem))
                .build();

        Page<Booking> page = new PageImpl<>(List.of(booking));
        Pageable pageable = PageRequest.of(0, 10);

        when(staffProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(staffProfile));
        when(bookingRepository.findCheckedInByCinemaAndDateRange(eq(cinema.getId()), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable))).thenReturn(page);

        PageResponse<CheckInHistoryItemResponse> result = staffDashboardService.getCheckInHistory(user, "TODAY", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("CODE123", result.getContent().get(0).getBookingCode());
        assertEquals("C5", result.getContent().get(0).getSeatsChecked());
    }

    @Test
    void testGetCheckInHistory_ThisMonth() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).build();
        StaffProfile staffProfile = StaffProfile.builder().cinema(cinema).build();

        Page<Booking> page = new PageImpl<>(List.of());
        Pageable pageable = PageRequest.of(0, 10);

        when(staffProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(staffProfile));
        when(bookingRepository.findCheckedInByCinemaAndDateRange(eq(cinema.getId()), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable))).thenReturn(page);

        PageResponse<CheckInHistoryItemResponse> result = staffDashboardService.getCheckInHistory(user, "THIS_MONTH", pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testGetCinemaId_NotFound() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(staffProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> staffDashboardService.getDashboardStats(user));
    }
}
