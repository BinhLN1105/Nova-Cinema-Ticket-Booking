package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.dto.request.OverrideSeatPriceRequest;
import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeSyncResponse;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.enums.ShowtimeStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.SeatMapper;
import com.cinema.ticket_booking.mapper.ShowtimeMapper;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private CinemaService cinemaService;
    @Mock
    private ShowtimeMapper showtimeMapper;
    @Mock
    private SeatMapper seatMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private SeatLockService seatLockService;
    @Mock
    private PricingEngineService pricingEngineService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ShowtimeServiceImpl showtimeService;

    @Test
    void testGetShowtimesForSync() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Avenger").build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).name("Cinema 1").build();
        Screen screen = Screen.builder().id(UUID.randomUUID()).name("Screen 1").cinema(cinema).build();
        Showtime s1 = Showtime.builder()
                .id(UUID.randomUUID())
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .build();

        when(systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10)).thenReturn(10);
        when(showtimeRepository.findAll()).thenReturn(List.of(s1));

        List<ShowtimeSyncResponse> list = showtimeService.getShowtimesForSync("Avenger", movie.getId(), "Cinema 1", LocalDate.now());
        assertEquals(1, list.size());
    }

    @Test
    void testGetByMovieAndDate() {
        UUID movieId = UUID.randomUUID();
        Showtime s = Showtime.builder().id(UUID.randomUUID()).startTime(LocalDateTime.now().plusHours(2)).build();
        ShowtimeResponse response = ShowtimeResponse.builder().build();

        when(systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10)).thenReturn(10);
        when(showtimeRepository.findByMovieAndDate(movieId, LocalDate.now())).thenReturn(List.of(s));
        when(showtimeMapper.toResponse(s)).thenReturn(response);
        when(showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE)).thenReturn(20L);

        List<ShowtimeResponse> list = showtimeService.getByMovieAndDate(movieId, LocalDate.now());
        assertEquals(1, list.size());
        assertEquals(20L, list.get(0).getAvailableSeats());
    }

    @Test
    void testGetByMovieCinemaAndDate() {
        UUID movieId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        Showtime s = Showtime.builder().id(UUID.randomUUID()).startTime(LocalDateTime.now().plusHours(2)).build();
        ShowtimeResponse response = ShowtimeResponse.builder().build();

        when(systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10)).thenReturn(10);
        when(showtimeRepository.findByMovieAndCinemaAndDate(movieId, cinemaId, LocalDate.now())).thenReturn(List.of(s));
        when(showtimeMapper.toResponse(s)).thenReturn(response);

        List<ShowtimeResponse> list = showtimeService.getByMovieCinemaAndDate(movieId, cinemaId, LocalDate.now());
        assertEquals(1, list.size());
    }

    @Test
    void testGetByCinemaAndDate() {
        UUID cinemaId = UUID.randomUUID();
        Showtime s = Showtime.builder().id(UUID.randomUUID()).startTime(LocalDateTime.now().plusHours(2)).build();
        ShowtimeResponse response = ShowtimeResponse.builder().build();

        when(systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10)).thenReturn(10);
        when(showtimeRepository.findByCinemaAndDateScheduled(cinemaId, LocalDate.now())).thenReturn(List.of(s));
        when(showtimeMapper.toResponse(s)).thenReturn(response);

        List<ShowtimeResponse> list = showtimeService.getByCinemaAndDate(cinemaId, LocalDate.now());
        assertEquals(1, list.size());
    }

    @Test
    void testGetById_Success() {
        UUID id = UUID.randomUUID();
        Showtime s = new Showtime();
        ShowtimeResponse response = ShowtimeResponse.builder().build();

        when(showtimeRepository.findById(id)).thenReturn(Optional.of(s));
        when(showtimeMapper.toResponse(s)).thenReturn(response);
        when(showtimeSeatRepository.countByShowtimeIdAndStatus(id, SeatStatus.AVAILABLE)).thenReturn(15L);

        ShowtimeResponse result = showtimeService.getById(id);
        assertNotNull(result);
        assertEquals(15L, result.getAvailableSeats());
    }

    @Test
    void testGetById_NotFound() {
        UUID id = UUID.randomUUID();
        when(showtimeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> showtimeService.getById(id));
    }

    @Test
    void testGetSeatMap_Success() {
        UUID showtimeId = UUID.randomUUID();
        Screen screen = Screen.builder().totalRows(10).totalCols(10).build();
        Showtime showtime = Showtime.builder().id(showtimeId).screen(screen).basePrice(BigDecimal.valueOf(100)).startTime(LocalDateTime.now().plusHours(1)).build();
        
        Seat seat1 = Seat.builder().id(UUID.randomUUID()).build();
        ShowtimeSeat ss1 = ShowtimeSeat.builder().id(UUID.randomUUID()).seat(seat1).build();
        
        PricingRule rule = new PricingRule();
        PricingResult result = new PricingResult(BigDecimal.valueOf(120), BigDecimal.valueOf(0), "");

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdWithSeat(showtimeId)).thenReturn(List.of(ss1));
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(List.of(rule));
        when(seatLockService.getLockedSeats(anyList())).thenReturn(List.of());
        
        SeatMapResponse.SeatItem seatItem = SeatMapResponse.SeatItem.builder()
                .status(SeatStatus.AVAILABLE)
                .rowLabel('A')
                .colNumber(1)
                .gridRow(0)
                .gridCol(0)
                .build();
        
        when(seatMapper.toSeatItem(ss1)).thenReturn(seatItem);
        when(pricingEngineService.calculateFinalSeatPrice(eq(showtime), eq(seat1), any(BigDecimal.class), anyList(), eq(1), eq(0))).thenReturn(result);
        
        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);

        SeatMapResponse response = showtimeService.getSeatMap(showtimeId);
        assertNotNull(response);
        assertEquals(10, response.getSeatHoldMins());
        assertEquals(120, response.getSeats().get(0).getPrice().intValue());
    }

    @Test
    void testCreate_Success() {
        ShowtimeRequest request = new ShowtimeRequest();
        request.setMovieId(UUID.randomUUID().toString());
        request.setScreenId(UUID.randomUUID().toString());
        request.setStartTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(2));
        request.setBasePrice(BigDecimal.valueOf(80));

        Movie movie = Movie.builder().duration(120).build();
        Screen screen = Screen.builder().id(UUID.fromString(request.getScreenId())).build();
        Seat seat = Seat.builder().id(UUID.randomUUID()).build();

        when(movieService.findById(any(UUID.class))).thenReturn(movie);
        when(screenRepository.findById(any(UUID.class))).thenReturn(Optional.of(screen));
        when(systemConfigService.getIntConfig("CLEANUP_TIME_MINUTES", 15)).thenReturn(15);
        when(showtimeRepository.existsConflict(eq(screen.getId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);
        when(seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscColNumberAsc(screen.getId())).thenReturn(List.of(seat));
        
        PricingResult pr = new PricingResult(BigDecimal.valueOf(80), BigDecimal.ZERO, "");
        when(pricingEngineService.calculateFinalSeatPrice(any(), eq(seat), any(), any(), eq(1), eq(0))).thenReturn(pr);

        ShowtimeResponse response = ShowtimeResponse.builder().build();
        when(showtimeMapper.toResponse(any(Showtime.class))).thenReturn(response);

        ShowtimeResponse result = showtimeService.create(request);
        assertNotNull(result);
    }

    @Test
    void testCreate_StartTimeInvalid() {
        ShowtimeRequest request = new ShowtimeRequest();
        request.setStartTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(1));

        assertThrows(BadRequestException.class, () -> showtimeService.create(request));
    }

    @Test
    void testCreate_Conflict() {
        ShowtimeRequest request = new ShowtimeRequest();
        request.setMovieId(UUID.randomUUID().toString());
        request.setScreenId(UUID.randomUUID().toString());
        request.setStartTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(2));
        request.setBasePrice(BigDecimal.valueOf(80));

        Movie movie = Movie.builder().duration(120).build();
        Screen screen = Screen.builder().id(UUID.fromString(request.getScreenId())).build();

        when(movieService.findById(any(UUID.class))).thenReturn(movie);
        when(screenRepository.findById(any(UUID.class))).thenReturn(Optional.of(screen));
        when(systemConfigService.getIntConfig("CLEANUP_TIME_MINUTES", 15)).thenReturn(15);
        when(showtimeRepository.existsConflict(eq(screen.getId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> showtimeService.create(request));
    }

    @Test
    void testAdminList() {
        Pageable pageable = PageRequest.of(0, 10);
        Showtime s = Showtime.builder().id(UUID.randomUUID()).build();
        Page<Showtime> page = new PageImpl<>(List.of(s));
        ShowtimeResponse response = ShowtimeResponse.builder().build();

        when(showtimeMapper.toResponse(s)).thenReturn(response);
        when(showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE)).thenReturn(10L);

        UUID cinemaId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(showtimeRepository.findByCinemaIdAndDate(cinemaId, date, pageable)).thenReturn(page);
        PageResponse<ShowtimeResponse> res1 = showtimeService.adminList(pageable, cinemaId.toString(), date);
        assertEquals(1, res1.getContent().size());

        when(showtimeRepository.findByCinemaId(cinemaId, pageable)).thenReturn(page);
        PageResponse<ShowtimeResponse> res2 = showtimeService.adminList(pageable, cinemaId.toString(), null);
        assertEquals(1, res2.getContent().size());

        when(showtimeRepository.findByDate(date, pageable)).thenReturn(page);
        PageResponse<ShowtimeResponse> res3 = showtimeService.adminList(pageable, null, date);
        assertEquals(1, res3.getContent().size());

        when(showtimeRepository.findAll(pageable)).thenReturn(page);
        PageResponse<ShowtimeResponse> res4 = showtimeService.adminList(pageable, null, null);
        assertEquals(1, res4.getContent().size());
    }

    @Test
    void testDelete_Success() {
        UUID id = UUID.randomUUID();
        Showtime showtime = Showtime.builder().id(id).build();
        Booking booking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.PENDING).build();
        Payment payment = new Payment();

        when(showtimeRepository.findById(id)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeId(id)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));

        showtimeService.delete(id);

        verify(paymentRepository).delete(payment);
        verify(bookingRepository).delete(booking);
        verify(showtimeSeatRepository).deleteByShowtimeId(id);
        verify(showtimeRepository).delete(showtime);
    }

    @Test
    void testDelete_Conflict() {
        UUID id = UUID.randomUUID();
        Showtime showtime = Showtime.builder().id(id).build();
        Booking booking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.PAID).build();

        when(showtimeRepository.findById(id)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeId(id)).thenReturn(List.of(booking));

        assertThrows(ConflictException.class, () -> showtimeService.delete(id));
    }

    @Test
    void testOverrideSeatPrices() {
        UUID showtimeId = UUID.randomUUID();
        OverrideSeatPriceRequest request = new OverrideSeatPriceRequest();
        UUID seatId = UUID.randomUUID();
        request.setShowtimeSeatIds(List.of(seatId));
        request.setNewPrice(BigDecimal.valueOf(150));

        Showtime showtime = new Showtime();
        ShowtimeSeat seat = new ShowtimeSeat();

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeAndIds(showtimeId, List.of(seatId))).thenReturn(List.of(seat));

        showtimeService.overrideSeatPrices(showtimeId, request);

        assertEquals(BigDecimal.valueOf(150), seat.getPrice());
        verify(showtimeSeatRepository).saveAll(List.of(seat));
    }
}
