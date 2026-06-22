package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.enums.*;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ForbiddenException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.exception.SeatAlreadyLockedException;
import com.cinema.ticket_booking.mapper.BookingMapper;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingItemRepository bookingItemRepository;
    @Mock private BookingComboRepository bookingComboRepository;
    @Mock private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private UserService userService;
    @Mock private ShowtimeService showtimeService;
    @Mock private VoucherService voucherService;
    @Mock private QrCodeService qrCodeService;
    @Mock private EmailService emailService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private StaffProfileRepository staffProfileRepository;
    @Mock private SeatLockService seatLockService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private PricingEngineService pricingEngineService;
    @Mock private UserVoucherRepository userVoucherRepository;
    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private UserExpHistoryRepository userExpHistoryRepository;
    @Mock private ScanLogRepository scanLogRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Showtime showtime;
    private BookingRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.CUSTOMER)
                .membershipTier(MembershipTier.BRONZE)
                .build();

        showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .status(ShowtimeStatus.SCHEDULED)
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(2))
                .basePrice(BigDecimal.valueOf(50000))
                .screen(Screen.builder().cinema(Cinema.builder().build()).build())
                .build();

        request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(UUID.randomUUID().toString()));
        request.setPaymentMethod(PaymentMethod.VNPAY);

        // Standard stubbings that prevent NPE
        lenient().when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(new ArrayList<>());
        lenient().when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().build());
        lenient().when(systemConfigService.getIntConfig("BOOKING_MAX_SEATS", 6)).thenReturn(6);
        lenient().when(systemConfigService.getIntConfig("BOOKING_MAX_COMBOS", 8)).thenReturn(8);
    }

    @Test
    void testCalculateQuote_ShowtimePast_ThrowsBadRequest() {
        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(1));

        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        assertThrows(BadRequestException.class, () -> bookingService.calculateQuote(user.getId(), request));
    }

    @Test
    void testCalculateQuote_ShowtimeNotScheduled_ThrowsBadRequest() {
        showtime.setStatus(ShowtimeStatus.CANCELLED);

        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        assertThrows(BadRequestException.class, () -> bookingService.calculateQuote(user.getId(), request));
    }

    @Test
    void testCreateBooking_SeatAlreadyLocked_ThrowsSeatAlreadyLockedException() {
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat1 = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(1).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        ShowtimeSeat seat2 = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(2).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat1.getId().toString(), seat2.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat1, seat2));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(seatLockService.lockSeat(eq(seat1.getId().toString()), anyString(), any(Duration.class))).thenReturn(true);
        when(seatLockService.lockSeat(eq(seat2.getId().toString()), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(SeatAlreadyLockedException.class, () -> bookingService.createBooking(user.getId(), request));

        verify(seatLockService).releaseSeats(argThat(list -> list.contains(seat1.getId().toString())));
    }

    @Test
    void testCreateBooking_LockExceptionRollsBackSuccessfully() {
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat1 = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(1).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        ShowtimeSeat seat2 = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(2).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat1.getId().toString(), seat2.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat1, seat2));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);

        when(seatLockService.lockSeat(eq(seat1.getId().toString()), anyString(), any(Duration.class))).thenReturn(true);
        when(seatLockService.lockSeat(eq(seat2.getId().toString()), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis error"));

        assertThrows(RuntimeException.class, () -> bookingService.createBooking(user.getId(), request));

        verify(seatLockService).releaseSeats(argThat(list -> list.contains(seat1.getId().toString())));
    }

    @Test
    void testCreateBooking_OnlineCustomerMissingShowtimeOrSeats_ThrowsBadRequestException() {
        request.setShowtimeId(null);
        request.setShowtimeSeatIds(null);
        
        when(userService.findById(any())).thenReturn(user);
        
        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCreateBooking_StaffMissingShowtimeAndCombo_ThrowsBadRequestException() {
        user.setRole(UserRole.STAFF);
        request.setShowtimeId(null);
        request.setShowtimeSeatIds(null);
        request.setCombos(null);
        
        when(userService.findById(any())).thenReturn(user);
        
        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCreateBooking_CashPaymentByNonStaff_ThrowsForbiddenException() {
        request.setPaymentMethod(PaymentMethod.CASH);
        
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.fromString(request.getShowtimeSeatIds().get(0)))
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        
        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        assertThrows(ForbiddenException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCreateBooking_SeatMismatch_ThrowsBadRequestException() {
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);
        
        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString(), UUID.randomUUID().toString()));
        
        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        
        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCreateBooking_LateSeatHoldTime_AppliesCorrectly() {
        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(5));
        
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3)).thenReturn(3);
        
        when(seatLockService.lockSeat(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder()
                .seats(List.of(BookingResponse.SeatItem.builder()
                        .showtimeSeatId(seat.getId().toString())
                        .rowLabel('A')
                        .colNumber(5)
                        .price(BigDecimal.valueOf(50000))
                        .seatType(SeatType.STANDARD.name())
                        .build()))
                .totalAmount(BigDecimal.valueOf(50000))
                .build());
        
        when(bookingRepository.save(any())).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        when(bookingItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(user.getId(), request));
        
        verify(seatLockService, atLeastOnce()).lockSeat(eq(seat.getId().toString()), anyString(), eq(Duration.ofMinutes(3)));
    }

    @Test
    void testCalculateQuote_MembershipRankDiscount_AppliesCorrectly() {
        user.setMembershipTier(MembershipTier.GOLD);
        user.setRankUsageThisMonth(0);
        
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        BookingResponse quote = bookingService.calculateQuote(user.getId(), request);
        assertNotNull(quote);
    }

    @Test
    void testCreateBooking_RankUsageExceeded_ThrowsBadRequestException() {
        user.setMembershipTier(MembershipTier.GOLD);
        user.setRankUsageThisMonth(MembershipTier.GOLD.getMaxUsage());
        
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));

        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));
        
        BookingResponse quoteResponse = BookingResponse.builder()
                .seats(List.of(BookingResponse.SeatItem.builder()
                        .showtimeSeatId(seat.getId().toString())
                        .rowLabel('A')
                        .colNumber(5)
                        .price(BigDecimal.valueOf(100000))
                        .seatType(SeatType.STANDARD.name())
                        .build()))
                .rankDiscountAmount(BigDecimal.valueOf(10000))
                .build();
        
        when(bookingMapper.toResponse(any())).thenReturn(quoteResponse);
        
        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCalculateQuote_ComboNotAvailable_ThrowsBadRequestException() {
        user.setRole(UserRole.STAFF); // set to staff so validation allows combo-only request
        
        when(userService.findById(any())).thenReturn(user);
        
        request.setShowtimeId(null);
        request.setShowtimeSeatIds(null);
        
        BookingRequest.ComboItem comboItem = new BookingRequest.ComboItem();
        comboItem.setComboId(UUID.randomUUID().toString());
        comboItem.setQuantity(2);
        request.setCombos(List.of(comboItem));
        
        Combo mockCombo = Combo.builder()
                .id(UUID.fromString(comboItem.getComboId()))
                .name("Popcorn")
                .price(BigDecimal.valueOf(30000))
                .isAvailable(false)
                .build();
        
        when(comboRepository.findById(any())).thenReturn(Optional.of(mockCombo));
        
        assertThrows(BadRequestException.class, () -> bookingService.calculateQuote(user.getId(), request));
    }

    @Test
    void testCreateBooking_SeatAlreadySold_ThrowsBadRequestException() {
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.BOOKED)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCalculateQuote_NoShowtimeAndNoCombo_ThrowsBadRequestException() {
        user.setRole(UserRole.STAFF);
        request.setShowtimeId(null);
        request.setShowtimeSeatIds(null);
        request.setCombos(null);

        when(userService.findById(any())).thenReturn(user);

        assertThrows(BadRequestException.class, () -> bookingService.calculateQuote(user.getId(), request));
    }

    @Test
    void testCalculateQuote_VoucherValidation_SetsWarningMessage() {
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));
        request.setVoucherCode("EXPIRED10");

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(voucherService.validateForOrder(any(), anyString(), any(BigDecimal.class)))
                .thenThrow(new BadRequestException("Voucher đã hết hạn"));

        BookingResponse quote = bookingService.calculateQuote(user.getId(), request);
        assertNotNull(quote);
        assertEquals("Mã giảm giá không hợp lệ hoặc đã hết hạn: Voucher đã hết hạn", quote.getWarningMessage());
    }

    @Test
    void testCreateBooking_AdminProcess_Success() {
        user.setRole(UserRole.ADMIN);
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(50000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(50000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(seatLockService.lockSeat(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        Booking mockSavedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .showtime(showtime)
                .totalAmount(BigDecimal.valueOf(50000))
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(mockSavedBooking);
        when(bookingItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.of(mockSavedBooking));

        BookingResponse mockResponse = BookingResponse.builder()
                .id(mockSavedBooking.getId().toString())
                .totalAmount(BigDecimal.valueOf(50000))
                .build();
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockResponse);

        BookingResponse response = bookingService.createBooking(user.getId(), request);
        assertNotNull(response);
        assertEquals(mockSavedBooking.getId().toString(), response.getId());
    }

    @Test
    void testCreateBooking_VoucherUsed_ThrowsBadRequestException() {
        request.setVoucherCode("VOUCHER100");
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("VOUCHER100")
                .discountValue(BigDecimal.valueOf(10000))
                .minOrder(BigDecimal.valueOf(50000))
                .build();

        when(voucherService.validateForOrder(any(), anyString(), any(BigDecimal.class))).thenReturn(voucher);

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.USED)
                .build();

        when(userVoucherRepository.findByUserIdAndVoucherId(any(), any())).thenReturn(Optional.of(userVoucher));
        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(seatLockService.lockSeat(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        BookingResponse mockQuoteResponse = BookingResponse.builder()
                .voucherCode("VOUCHER100")
                .subtotal(BigDecimal.valueOf(100000))
                .totalAmount(BigDecimal.valueOf(90000))
                .build();
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockQuoteResponse);

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(user.getId(), request));
    }

    @Test
    void testCreateBooking_VoucherPending_Success() {
        request.setVoucherCode("VOUCHER100");
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("VOUCHER100")
                .discountValue(BigDecimal.valueOf(10000))
                .minOrder(BigDecimal.valueOf(50000))
                .build();

        when(voucherService.validateForOrder(any(), anyString(), any(BigDecimal.class))).thenReturn(voucher);

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.AVAILABLE)
                .build();

        when(userVoucherRepository.findByUserIdAndVoucherId(any(), any())).thenReturn(Optional.of(userVoucher));
        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(seatLockService.lockSeat(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        Booking mockSavedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .showtime(showtime)
                .totalAmount(BigDecimal.valueOf(90000))
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(mockSavedBooking);
        when(bookingItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.of(mockSavedBooking));

        BookingResponse mockResponse = BookingResponse.builder()
                .id(mockSavedBooking.getId().toString())
                .voucherCode("VOUCHER100")
                .subtotal(BigDecimal.valueOf(100000))
                .totalAmount(BigDecimal.valueOf(90000))
                .build();
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockResponse);

        BookingResponse response = bookingService.createBooking(user.getId(), request);
        assertNotNull(response);
        assertEquals(UserVoucherStatus.PENDING, userVoucher.getStatus());
        verify(userVoucherRepository).save(userVoucher);
    }

    @Test
    void testCreateBooking_CashPaymentByStaff_Success() {
        user.setRole(UserRole.STAFF);
        user.setMembershipTier(MembershipTier.GOLD);
        user.setRankUsageThisMonth(0);

        request.setPaymentMethod(PaymentMethod.CASH);
        when(userService.findById(any())).thenReturn(user);
        when(showtimeService.findById(any())).thenReturn(showtime);
        when(staffProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.AVAILABLE)
                .seat(Seat.builder().rowLabel('A').colNumber(5).seatType(SeatType.STANDARD).build())
                .price(BigDecimal.valueOf(100000))
                .build();
        request.setShowtimeSeatIds(List.of(seat.getId().toString()));

        when(showtimeSeatRepository.findByShowtimeAndIds(any(), anyList())).thenReturn(List.of(seat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(BigDecimal.valueOf(100000), BigDecimal.ZERO, null));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null));

        when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
        when(seatLockService.lockSeat(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(qrCodeService.generateQrContent(any())).thenReturn("MOCK_QR");

        Booking mockSavedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .showtime(showtime)
                .rankDiscountAmount(BigDecimal.valueOf(10000))
                .totalAmount(BigDecimal.valueOf(90000))
                .status(BookingStatus.PAID)
                .build();

        // The save method is called multiple times: first for initial save, second for paid status update (setting QR code)
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockSavedBooking);
        when(bookingItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.of(mockSavedBooking));

        BookingResponse mockResponse = BookingResponse.builder()
                .id(mockSavedBooking.getId().toString())
                .totalAmount(BigDecimal.valueOf(90000))
                .build();
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockResponse);

        BookingResponse response = bookingService.createBooking(user.getId(), request);
        assertNotNull(response);
        assertEquals(1, user.getRankUsageThisMonth());
        verify(userService).save(user);
    }
}
