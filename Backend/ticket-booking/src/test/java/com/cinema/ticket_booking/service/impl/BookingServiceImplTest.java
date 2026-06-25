package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.CheckInResponse;
import com.cinema.ticket_booking.enums.*;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
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

    @Test
    void testConfirmPaid_Success() {
        UUID bookingId = UUID.randomUUID();
        User bookingUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test Customer")
                .availableExp(100L)
                .membershipTier(MembershipTier.BRONZE)
                .build();

        Seat seat = Seat.builder().rowLabel('A').colNumber(1).build();
        ShowtimeSeat showtimeSeat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.LOCKED)
                .seat(seat)
                .showtime(showtime)
                .build();

        BookingItem item = BookingItem.builder()
                .showtimeSeat(showtimeSeat)
                .build();

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PENDING)
                .user(bookingUser)
                .earnedExp(50L)
                .rankDiscountAmount(BigDecimal.valueOf(10000))
                .voucher(voucher)
                .bookingItems(List.of(item))
                .bookingCombos(new ArrayList<>())
                .cinema(Cinema.builder().name("Nova Cinema").build())
                .showtime(showtime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findByBookingIdWithSeat(bookingId)).thenReturn(List.of(item));
        when(qrCodeService.generateQrContent(any())).thenReturn("MOCK_QR");

        UserVoucher userVoucher = UserVoucher.builder()
                .status(UserVoucherStatus.PENDING)
                .build();
        when(userVoucherRepository.findByUserIdAndVoucherId(any(), any())).thenReturn(Optional.of(userVoucher));

        bookingService.confirmPaid(bookingId);

        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals("MOCK_QR", booking.getQrCode());
        assertEquals(150L, bookingUser.getAvailableExp());
        assertEquals(1, bookingUser.getRankUsageThisMonth());
        assertEquals(UserVoucherStatus.USED, userVoucher.getStatus());

        verify(showtimeSeatRepository).saveAll(anyList());
        verify(seatLockService).releaseSeats(anyList());
        verify(userVoucherRepository).save(userVoucher);
        verify(userExpHistoryRepository).save(any(UserExpHistory.class));
        verify(bookingRepository).save(booking);
    }

    @Test
    void testConfirmPaid_AlreadyProcessed_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.PAID)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.confirmPaid(bookingId));
    }

    @Test
    void testCancelBooking_Success_Customer() {
        UUID bookingId = UUID.randomUUID();
        User bookingUser = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.CUSTOMER)
                .availableExp(150L)
                .rewardPoints(100L)
                .build();

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .build();

        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(3));

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PAID)
                .user(bookingUser)
                .showtime(showtime)
                .expAdded(true)
                .earnedExp(50L)
                .rankDiscountAmount(BigDecimal.valueOf(10000))
                .voucher(voucher)
                .totalAmount(BigDecimal.valueOf(100000))
                .bookingItems(new ArrayList<>())
                .bookingCombos(new ArrayList<>())
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("true");
        when(systemConfigService.getIntConfig("REFUND_PERCENT_CINEPOINT", 100)).thenReturn(80);

        UserVoucher userVoucher = UserVoucher.builder()
                .status(UserVoucherStatus.USED)
                .build();
        when(userVoucherRepository.findByUserIdAndVoucherId(any(), any())).thenReturn(Optional.of(userVoucher));

        bookingService.cancelBooking(bookingUser, bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNull(booking.getCancellationToken());
        assertFalse(booking.getExpAdded());
        assertEquals(100L, bookingUser.getAvailableExp()); // Decreases to 100L because earnedExp (50L) is correctly revoked
        assertEquals(0, bookingUser.getRankUsageThisMonth());
        assertEquals(UserVoucherStatus.AVAILABLE, userVoucher.getStatus());
        assertEquals(180L, bookingUser.getRewardPoints()); // 100 + (100000 * 80% / 100 / 1000) = 180

        verify(bookingRepository).save(booking);
        verify(userVoucherRepository).save(userVoucher);
        verify(userService).save(bookingUser);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCancelBooking_Success_Staff() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.CUSTOMER)
                .availableExp(100L)
                .rewardPoints(100L)
                .build();

        User staff = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.STAFF)
                .fullName("Staff Member")
                .build();

        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(30));

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PAID)
                .user(customer)
                .showtime(showtime)
                .expAdded(false)
                .totalAmount(BigDecimal.valueOf(100000))
                .bookingItems(new ArrayList<>())
                .bookingCombos(new ArrayList<>())
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("true");

        bookingService.cancelBooking(staff, bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(200L, customer.getRewardPoints()); // Staff cancel -> 100% refund -> 100 + 100 = 200

        verify(bookingRepository).save(booking);
        verify(userService).save(customer);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCancelBooking_NoPermission_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer1 = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();
        User customer2 = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer1)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(customer2, bookingId));
    }

    @Test
    void testCancelBooking_NotPaid_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(customer, bookingId));
    }

    @Test
    void testCancelBooking_LateCancellation_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(1));

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PAID)
                .showtime(showtime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("true");

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(customer, bookingId));
    }

    @Test
    void testCancelConfirm_Success() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.CUSTOMER)
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PAID)
                .showtime(showtime)
                .cancellationToken("CONFIRM_TOKEN")
                .cancellationTokenExpiry(LocalDateTime.now().plusMinutes(5))
                .bookingItems(new ArrayList<>())
                .bookingCombos(new ArrayList<>())
                .totalAmount(BigDecimal.valueOf(100000))
                .build();

        when(bookingRepository.findByIdWithUser(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("false");

        bookingService.cancelConfirm("CONFIRM_TOKEN", bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNull(booking.getCancellationToken());
    }

    @Test
    void testCancelConfirm_InvalidToken_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .cancellationToken("CONFIRM_TOKEN")
                .build();

        when(bookingRepository.findByIdWithUser(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelConfirm("WRONG_TOKEN", bookingId));
    }

    @Test
    void testCancelConfirm_ExpiredToken_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .cancellationToken("CONFIRM_TOKEN")
                .cancellationTokenExpiry(LocalDateTime.now().minusMinutes(5))
                .build();

        when(bookingRepository.findByIdWithUser(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelConfirm("CONFIRM_TOKEN", bookingId));
    }

    @Test
    void testCancelRequest_Success() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test Customer")
                .role(UserRole.CUSTOMER)
                .build();

        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(3));

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PAID)
                .user(customer)
                .showtime(showtime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("true");

        bookingService.cancelRequest(customer.getId(), bookingId);

        assertNotNull(booking.getCancellationToken());
        assertNotNull(booking.getCancellationTokenExpiry());
        verify(bookingRepository).save(booking);
        verify(emailService).sendCancellationRequestEmail(eq("test@example.com"), eq("Test Customer"), eq("BK123"), anyString(), eq(bookingId), anyString());
    }

    @Test
    void testCancelRequest_NoPermission_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer1 = User.builder().id(UUID.randomUUID()).build();
        User customer2 = User.builder().id(UUID.randomUUID()).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer1)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelRequest(customer2.getId(), bookingId));
    }

    @Test
    void testCancelRequest_NotPaid_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelRequest(customer.getId(), bookingId));
    }

    @Test
    void testCancelRequest_Late_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).build();

        showtime.setStartTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(1));

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PAID)
                .showtime(showtime)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2)).thenReturn(2);
        when(systemConfigService.getConfig("ENABLE_CANCEL_TIME_CHECK", "false")).thenReturn("true");

        assertThrows(BadRequestException.class, () -> bookingService.cancelRequest(customer.getId(), bookingId));
    }

    @Test
    void testGetCancelToken_Success() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PAID)
                .cancellationToken("CANCEL_TOKEN")
                .cancellationTokenExpiry(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(5))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userService.findById(customer.getId())).thenReturn(customer);

        String token = bookingService.getCancelToken(customer.getId(), bookingId);
        assertEquals("CANCEL_TOKEN", token);
    }

    @Test
    void testGetCancelToken_Forbidden_ThrowsForbiddenException() {
        UUID bookingId = UUID.randomUUID();
        User customer1 = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();
        User customer2 = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer1)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userService.findById(customer2.getId())).thenReturn(customer2);

        assertThrows(ForbiddenException.class, () -> bookingService.getCancelToken(customer2.getId(), bookingId));
    }

    @Test
    void testGetCancelToken_NotPaid_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userService.findById(customer.getId())).thenReturn(customer);

        assertThrows(BadRequestException.class, () -> bookingService.getCancelToken(customer.getId(), bookingId));
    }

    @Test
    void testGetCancelToken_ExpiredOrNotCreated_ThrowsBadRequestException() {
        UUID bookingId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(customer)
                .status(BookingStatus.PAID)
                .cancellationTokenExpiry(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(5))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userService.findById(customer.getId())).thenReturn(customer);

        assertThrows(BadRequestException.class, () -> bookingService.getCancelToken(customer.getId(), bookingId));
    }

    @Test
    void testCheckIn_Success_Staff() {
        UUID bookingId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();

        User staff = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.STAFF)
                .fullName("Staff One")
                .build();

        Cinema cinema = Cinema.builder().id(cinemaId).name("Nova Cinema").build();
        Screen screen = Screen.builder().name("Room 1").cinema(cinema).build();
        Movie movie = Movie.builder().title("Movie 1").posterUrl("poster.jpg").build();
        Showtime localShowtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusHours(1))
                .screen(screen)
                .movie(movie)
                .build();

        User customer = User.builder().id(UUID.randomUUID()).fullName("Customer One").build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PAID)
                .showtime(localShowtime)
                .user(customer)
                .build();

        Seat seat = Seat.builder().rowLabel('A').colNumber(1).seatType(SeatType.STANDARD).build();
        ShowtimeSeat ss = ShowtimeSeat.builder().seat(seat).build();
        BookingItem item = BookingItem.builder().showtimeSeat(ss).build();
        Ticket ticket = Ticket.builder().bookingItem(item).isUsed(false).build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingIdWithSeatDetail(bookingId)).thenReturn(List.of(ticket));

        StaffProfile profile = StaffProfile.builder().cinema(cinema).build();
        when(staffProfileRepository.findByUserId(staff.getId())).thenReturn(Optional.of(profile));

        CheckInResponse response = bookingService.checkIn(staff, "BK123");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertTrue(ticket.getIsUsed());
        verify(ticketRepository).saveAll(anyList());
        verify(bookingRepository).save(booking);
        verify(scanLogRepository).save(any(ScanLog.class));
    }

    @Test
    void testCheckIn_Success_Admin() {
        UUID bookingId = UUID.randomUUID();

        User admin = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.ADMIN)
                .fullName("Admin One")
                .build();

        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).name("Nova Cinema").build();
        Screen screen = Screen.builder().name("Room 1").cinema(cinema).build();
        Movie movie = Movie.builder().title("Movie 1").posterUrl("poster.jpg").build();
        Showtime localShowtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusHours(1))
                .screen(screen)
                .movie(movie)
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123")
                .status(BookingStatus.PAID)
                .showtime(localShowtime)
                .build();

        Seat seat = Seat.builder().rowLabel('A').colNumber(1).seatType(SeatType.STANDARD).build();
        ShowtimeSeat ss = ShowtimeSeat.builder().seat(seat).build();
        BookingItem item = BookingItem.builder().showtimeSeat(ss).build();
        Ticket ticket = Ticket.builder().bookingItem(item).isUsed(false).build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingIdWithSeatDetail(bookingId)).thenReturn(List.of(ticket));

        CheckInResponse response = bookingService.checkIn(admin, "BK123");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void testCheckIn_NotFound_ThrowsBadRequestException() {
        User admin = User.builder().role(UserRole.ADMIN).build();
        when(bookingRepository.findByQrCode("INVALID")).thenReturn(Optional.empty());
        when(bookingRepository.findByBookingCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> bookingService.checkIn(admin, "INVALID"));
    }

    @Test
    void testCheckIn_AlreadyCheckedIn_ThrowsConflictException() {
        User admin = User.builder().role(UserRole.ADMIN).build();
        Booking booking = Booking.builder().status(BookingStatus.CHECKED_IN).build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> bookingService.checkIn(admin, "BK123"));
    }

    @Test
    void testCheckIn_NotPaid_ThrowsBadRequestException() {
        User admin = User.builder().role(UserRole.ADMIN).build();
        Booking booking = Booking.builder().status(BookingStatus.PENDING).build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.checkIn(admin, "BK123"));
    }

    @Test
    void testCheckIn_InvalidDate_ThrowsBadRequestException() {
        User admin = User.builder().role(UserRole.ADMIN).build();

        Showtime localShowtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusDays(40)) // Over 30 days limit
                .build();

        Booking booking = Booking.builder()
                .status(BookingStatus.PAID)
                .showtime(localShowtime)
                .build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.checkIn(admin, "BK123"));
    }

    @Test
    void testCheckIn_StaffNoProfile_ThrowsForbiddenException() {
        User staff = User.builder().id(UUID.randomUUID()).role(UserRole.STAFF).build();
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).build();
        Screen screen = Screen.builder().cinema(cinema).build();
        Showtime localShowtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusHours(1))
                .screen(screen)
                .build();

        Booking booking = Booking.builder()
                .status(BookingStatus.PAID)
                .showtime(localShowtime)
                .build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));
        when(staffProfileRepository.findByUserId(staff.getId())).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> bookingService.checkIn(staff, "BK123"));
    }

    @Test
    void testCheckIn_StaffWrongCinema_ThrowsForbiddenException() {
        User staff = User.builder().id(UUID.randomUUID()).role(UserRole.STAFF).build();
        Cinema cinema1 = Cinema.builder().id(UUID.randomUUID()).build();
        Cinema cinema2 = Cinema.builder().id(UUID.randomUUID()).build();

        Screen screen = Screen.builder().cinema(cinema1).build();
        Showtime localShowtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusHours(1))
                .screen(screen)
                .build();

        Booking booking = Booking.builder()
                .status(BookingStatus.PAID)
                .showtime(localShowtime)
                .build();

        when(bookingRepository.findByQrCode("BK123")).thenReturn(Optional.of(booking));

        StaffProfile profile = StaffProfile.builder().cinema(cinema2).build();
        when(staffProfileRepository.findByUserId(staff.getId())).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> bookingService.checkIn(staff, "BK123"));
    }

    @Test
    void testGetEligibleBookingForReview_Found() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Booking booking = Booking.builder().id(UUID.randomUUID()).build();

        when(bookingRepository.findEligibleBookingForReview(userId, movieId)).thenReturn(Optional.of(booking));

        UUID eligibleId = bookingService.getEligibleBookingForReview(userId, movieId);
        assertEquals(booking.getId(), eligibleId);
    }

    @Test
    void testGetEligibleBookingForReview_NotFound() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(bookingRepository.findEligibleBookingForReview(userId, movieId)).thenReturn(Optional.empty());

        UUID eligibleId = bookingService.getEligibleBookingForReview(userId, movieId);
        assertNull(eligibleId);
    }
}
