package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.mapper.SeatMapper;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.cinema.ticket_booking.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatMapServiceImplTest {

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    private ShowtimeService showtimeService;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SeatMapServiceImpl seatMapService;

    private UUID showtimeId;
    private Showtime showtime;
    private Screen screen;

    @BeforeEach
    void setUp() {
        showtimeId = UUID.randomUUID();
        screen = Screen.builder()
                .id(UUID.randomUUID())
                .totalRows(10)
                .totalCols(10)
                .build();

        showtime = Showtime.builder()
                .id(showtimeId)
                .screen(screen)
                .startTime(LocalDateTime.now().plusHours(2))
                .build();
    }

    @Test
    void testGetSeatMap_NormalHoldTime() {
        when(showtimeService.findById(showtimeId)).thenReturn(showtime);

        ShowtimeSeat seat1 = ShowtimeSeat.builder().id(UUID.randomUUID()).build();
        when(showtimeSeatRepository.findByShowtimeIdWithSeat(showtimeId)).thenReturn(List.of(seat1));

        SeatMapResponse.SeatItem item1 = SeatMapResponse.SeatItem.builder()
                .gridRow(1)
                .gridCol(2)
                .build();
        when(seatMapper.toSeatItem(any())).thenReturn(item1);

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3)).thenReturn(3);

        SeatMapResponse response = seatMapService.getSeatMap(showtimeId);

        assertNotNull(response);
        assertEquals(showtimeId.toString(), response.getShowtimeId());
        assertEquals(10, response.getSeatHoldMins());
        assertEquals(1, response.getMaxGridRow());
        assertEquals(2, response.getMaxGridCol());
    }

    @Test
    void testGetSeatMap_LateHoldTimeAndDummyCorrection() {
        // Set start time to 5 minutes in the future (within late hold window <= 15 and >= -10)
        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(5));
        when(showtimeService.findById(showtimeId)).thenReturn(showtime);

        ShowtimeSeat seat1 = ShowtimeSeat.builder().id(UUID.randomUUID()).build();
        ShowtimeSeat seat2 = ShowtimeSeat.builder().id(UUID.randomUUID()).build();
        when(showtimeSeatRepository.findByShowtimeIdWithSeat(showtimeId)).thenReturn(List.of(seat1, seat2));

        // item1 requires letter mapping correction
        SeatMapResponse.SeatItem item1 = SeatMapResponse.SeatItem.builder()
                .gridRow(0)
                .gridCol(0)
                .rowLabel('C') // 'C' - 'A' = 2
                .colNumber(5)  // 5 - 1 = 4
                .build();

        // item2 has null row label, requires dummy grid allocation
        SeatMapResponse.SeatItem item2 = SeatMapResponse.SeatItem.builder()
                .gridRow(0)
                .gridCol(0)
                .rowLabel(null)
                .colNumber(0)
                .build();

        when(seatMapper.toSeatItem(seat1)).thenReturn(item1);
        when(seatMapper.toSeatItem(seat2)).thenReturn(item2);

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3)).thenReturn(3);

        SeatMapResponse response = seatMapService.getSeatMap(showtimeId);

        assertNotNull(response);
        assertEquals(3, response.getSeatHoldMins());
        // item1 corrected
        assertEquals(2, item1.getGridRow());
        assertEquals(4, item1.getGridCol());
        // item2 dummy allocation corrected
        assertEquals(0, item2.getGridRow());
        assertEquals(0, item2.getGridCol());
    }
}
