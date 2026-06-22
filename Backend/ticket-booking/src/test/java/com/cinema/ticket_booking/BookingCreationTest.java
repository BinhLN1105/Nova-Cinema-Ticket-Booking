package com.cinema.ticket_booking;

import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.enums.*;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.exception.SeatAlreadyLockedException;
import com.cinema.ticket_booking.mapper.BookingMapper;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.*;
import com.cinema.ticket_booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCreationTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private BookingComboRepository bookingComboRepository;
    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ComboRepository comboRepository;
    @Mock
    private UserService userService;
    @Mock
    private ShowtimeService showtimeService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private QrCodeService qrCodeService;
    @Mock
    private EmailService emailService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private SeatLockService seatLockService;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private PricingEngineService pricingEngineService;
    @Mock
    private UserVoucherRepository userVoucherRepository;
    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @Mock
    private UserExpHistoryRepository userExpHistoryRepository;
    @Mock
    private ScanLogRepository scanLogRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID userId;
    private User customerUser;
    private User staffUser;
    private Showtime showtime;
    private ShowtimeSeat showtimeSeat;
    private Seat seat;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        customerUser = User.builder()
                .id(userId)
                .email("customer@example.com")
                .fullName("Customer User")
                .role(UserRole.CUSTOMER)
                .membershipTier(MembershipTier.BRONZE)
                .rankUsageThisMonth(0)
                .availableExp(100L)
                .rewardPoints(0L)
                .build();

        staffUser = User.builder()
                .id(UUID.randomUUID())
                .email("staff@example.com")
                .fullName("Staff User")
                .role(UserRole.STAFF)
                .membershipTier(MembershipTier.BRONZE)
                .rankUsageThisMonth(0)
                .availableExp(100L)
                .rewardPoints(0L)
                .build();

        Cinema cinema = Cinema.builder()
                .id(UUID.randomUUID())
                .name("Cine Cinema")
                .build();

        Screen screen = Screen.builder()
                .id(UUID.randomUUID())
                .name("Screen 1")
                .cinema(cinema)
                .screenType(ScreenType.STANDARD)
                .build();

        Movie movie = Movie.builder()
                .id(UUID.randomUUID())
                .title("Awesome Movie")
                .duration(120)
                .build();

        showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .basePrice(new BigDecimal("80000"))
                .status(ShowtimeStatus.SCHEDULED)
                .build();

        seat = Seat.builder()
                .id(UUID.randomUUID())
                .rowLabel('A')
                .colNumber(1)
                .seatType(SeatType.STANDARD)
                .build();

        showtimeSeat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .showtime(showtime)
                .seat(seat)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("80000"))
                .build();

        // Global mapper stubbing
        lenient().when(bookingMapper.toResponse(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            BookingResponse res = new BookingResponse();
            if (b != null) {
                res.setId(b.getId() != null ? b.getId().toString() : null);
                res.setBookingCode(b.getBookingCode());
                res.setTotalAmount(b.getTotalAmount());
                res.setDiscountAmount(b.getDiscountAmount());
                res.setPromotionDiscountAmount(b.getPromotionDiscountAmount());
                res.setRankDiscountAmount(b.getRankDiscountAmount());
                res.setAppliedPromotionName(b.getAppliedPromotionName());
                res.setStatus(b.getStatus());
            }
            return res;
        });
    }

    private void mockSystemConfigs() {
        lenient().when(systemConfigService.getIntConfig("BOOKING_MAX_SEATS", 6)).thenReturn(6);
        lenient().when(systemConfigService.getIntConfig("BOOKING_MAX_COMBOS", 8)).thenReturn(8);
        lenient().when(systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10)).thenReturn(10);
    }

    // ── 1. ĐẶT VÉ THÀNH CÔNG ──────────────────────────────────────────────

    @Test
    void testCalculateQuote_Success_Customer() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(showtimeSeatRepository.findByShowtimeAndIds(eq(showtime.getId()), anyList()))
                .thenReturn(List.of(showtimeSeat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(new BigDecimal("80000"), BigDecimal.ZERO, "None"));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, "None"));

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));

        BookingResponse response = bookingService.calculateQuote(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("80000"), response.getSubtotal());
        assertEquals(1, response.getSeats().size());
        verify(showtimeSeatRepository).findByShowtimeAndIds(eq(showtime.getId()), anyList());
    }

    @Test
    void testCalculateQuote_Success_Staff_ComboOnly() {
        mockSystemConfigs();
        when(userService.findById(staffUser.getId())).thenReturn(staffUser);
        
        UUID comboId = UUID.randomUUID();
        Combo combo = Combo.builder()
                .id(comboId)
                .name("Popcorn Combo")
                .price(new BigDecimal("50000"))
                .isAvailable(true)
                .build();
        when(comboRepository.findById(comboId)).thenReturn(Optional.of(combo));

        BookingRequest request = new BookingRequest();
        BookingRequest.ComboItem item = new BookingRequest.ComboItem();
        item.setComboId(comboId.toString());
        item.setQuantity(2);
        request.setCombos(List.of(item));

        BookingResponse response = bookingService.calculateQuote(staffUser.getId(), request);

        assertNotNull(response);
        assertEquals(new BigDecimal("100000"), response.getTotalOriginalAmount());
        assertEquals(1, response.getCombos().size());
        verify(comboRepository).findById(comboId);
    }

    @Test
    void testCreateBooking_Success() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(showtimeSeatRepository.findByShowtimeAndIds(eq(showtime.getId()), anyList()))
                .thenReturn(List.of(showtimeSeat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(new BigDecimal("80000"), BigDecimal.ZERO, "None"));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, "None"));
        
        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .showtime(showtime)
                .bookingCode("BK123456")
                .totalAmount(new BigDecimal("80000"))
                .status(BookingStatus.PENDING)
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        when(seatLockService.lockSeat(anyString(), anyString(), any())).thenReturn(true);

        BookingItem savedItem = BookingItem.builder()
                .id(UUID.randomUUID())
                .booking(savedBooking)
                .showtimeSeat(showtimeSeat)
                .seatPrice(new BigDecimal("80000"))
                .build();
        when(bookingItemRepository.saveAll(anyList())).thenReturn(List.of(savedItem));

        // Mocks for getDetail
        when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));
        when(bookingItemRepository.findByBookingIdWithSeat(savedBooking.getId())).thenReturn(List.of(savedItem));
        when(bookingComboRepository.findByBookingId(savedBooking.getId())).thenReturn(Collections.emptyList());

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));
        request.setPaymentMethod(PaymentMethod.VNPAY);

        BookingResponse response = bookingService.createBooking(userId, request);

        assertNotNull(response);
        verify(bookingRepository, atLeastOnce()).save(any(Booking.class));
        verify(seatLockService, times(2)).lockSeat(eq(showtimeSeat.getId().toString()), anyString(), any());
        verify(bookingItemRepository).saveAll(anyList());
        verify(ticketRepository).saveAll(anyList());
    }

    @Test
    void testCreateBooking_Success_Staff() {
        mockSystemConfigs();
        when(userService.findById(staffUser.getId())).thenReturn(staffUser);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);
        
        Cinema staffCinema = showtime.getScreen().getCinema();
        StaffProfile staffProfile = StaffProfile.builder()
                .id(UUID.randomUUID())
                .user(staffUser)
                .cinema(staffCinema)
                .build();
        when(staffProfileRepository.findByUserId(staffUser.getId())).thenReturn(Optional.of(staffProfile));
        
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(showtimeSeatRepository.findByShowtimeAndIds(eq(showtime.getId()), anyList()))
                .thenReturn(List.of(showtimeSeat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(new BigDecimal("80000"), BigDecimal.ZERO, "None"));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, "None"));
        
        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(staffUser)
                .showtime(showtime)
                .cinema(staffCinema)
                .bookingCode("BK123457")
                .totalAmount(new BigDecimal("80000"))
                .status(BookingStatus.PAID)
                .processedBy(staffUser)
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        when(seatLockService.lockSeat(anyString(), anyString(), any())).thenReturn(true);

        BookingItem savedItem = BookingItem.builder()
                .id(UUID.randomUUID())
                .booking(savedBooking)
                .showtimeSeat(showtimeSeat)
                .seatPrice(new BigDecimal("80000"))
                .build();
        when(bookingItemRepository.saveAll(anyList())).thenReturn(List.of(savedItem));

        // Mocks for getDetail
        when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));
        when(bookingItemRepository.findByBookingIdWithSeat(savedBooking.getId())).thenReturn(List.of(savedItem));
        when(bookingComboRepository.findByBookingId(savedBooking.getId())).thenReturn(Collections.emptyList());

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));
        request.setPaymentMethod(PaymentMethod.CASH);

        BookingResponse response = bookingService.createBooking(staffUser.getId(), request);

        assertNotNull(response);
        verify(staffProfileRepository).findByUserId(staffUser.getId());
        verify(bookingRepository, atLeastOnce()).save(any(Booking.class));
        verify(seatLockService, times(2)).lockSeat(eq(showtimeSeat.getId().toString()), anyString(), any());
        verify(bookingItemRepository).saveAll(anyList());
        verify(ticketRepository).saveAll(anyList());
    }

    // ── 2. THẤT BẠI DO GHẾ ĐÃ BỊ KHÓA (SeatAlreadyLockedException) ──────────

    @Test
    void testCreateBooking_SeatAlreadyLockedException() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(showtimeSeatRepository.findByShowtimeAndIds(eq(showtime.getId()), anyList()))
                .thenReturn(List.of(showtimeSeat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(new BigDecimal("80000"), BigDecimal.ZERO, "None"));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, "None"));
        
        when(seatLockService.lockSeat(anyString(), anyString(), any())).thenReturn(false);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));
        request.setPaymentMethod(PaymentMethod.VNPAY);

        assertThrows(SeatAlreadyLockedException.class, () -> {
            bookingService.createBooking(userId, request);
        });

        verify(seatLockService).releaseSeats(anyList());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ── 3. THẤT BẠI DO SUẤT CHIẾU KHÔNG TỒN TẠI HOẶC ĐÃ QUA GIỜ CHIẾU ───────

    @Test
    void testCalculateQuote_ShowtimeNotFound() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        
        UUID nonExistShowtimeId = UUID.randomUUID();
        when(showtimeService.findById(nonExistShowtimeId)).thenThrow(new ResourceNotFoundException("Suất chiếu", nonExistShowtimeId));

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(nonExistShowtimeId.toString());
        request.setShowtimeSeatIds(List.of(UUID.randomUUID().toString()));

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.calculateQuote(userId, request);
        });
    }

    @Test
    void testCalculateQuote_ShowtimePassed() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        
        showtime.setStartTime(LocalDateTime.now().minusHours(2));
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(userId, request);
        });
        assertEquals("Suất chiếu không còn nhận đặt vé", ex.getMessage());
    }

    @Test
    void testCalculateQuote_ShowtimeNotScheduled() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        
        showtime.setStatus(ShowtimeStatus.FINISHED);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(userId, request);
        });
        assertEquals("Suất chiếu không còn nhận đặt vé", ex.getMessage());
    }

    // ── 4. VALIDATE ĐỊNH DẠNG DỮ LIỆU ĐẦU VÀO CỦA REQUEST ĐẶT VÉ ───────────

    @Test
    void testCalculateQuote_ValidationError_CustomerMissingSeats() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(userId, request);
        });
        assertEquals("Khách hàng đặt vé bắt buộc phải chọn Suất chiếu và Ghế", ex.getMessage());
    }

    @Test
    void testCalculateQuote_ValidationError_StaffMissingAll() {
        mockSystemConfigs();
        when(userService.findById(staffUser.getId())).thenReturn(staffUser);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(null);
        request.setCombos(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(staffUser.getId(), request);
        });
        assertEquals("Phải chọn ít nhất 1 Suất chiếu hoặc 1 Combo bắp nước", ex.getMessage());
    }

    @Test
    void testCalculateQuote_ValidationError_NoSelection() {
        mockSystemConfigs();
        when(userService.findById(staffUser.getId())).thenReturn(staffUser);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(null);
        request.setCombos(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(staffUser.getId(), request);
        });
        assertEquals("Phải chọn ít nhất 1 Suất chiếu hoặc 1 Combo bắp nước", ex.getMessage());
    }

    @Test
    void testCalculateQuote_ValidationError_MaxSeatsExceeded() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of("1", "2", "3", "4", "5", "6", "7"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(userId, request);
        });
        assertEquals("Mỗi giao dịch chỉ được đặt tối đa 6 ghế.", ex.getMessage());
    }

    @Test
    void testCalculateQuote_ValidationError_MaxCombosExceeded() {
        mockSystemConfigs();
        when(userService.findById(staffUser.getId())).thenReturn(staffUser);

        BookingRequest request = new BookingRequest();
        BookingRequest.ComboItem item = new BookingRequest.ComboItem();
        item.setComboId(UUID.randomUUID().toString());
        item.setQuantity(9);
        request.setCombos(List.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.calculateQuote(staffUser.getId(), request);
        });
        assertEquals("Mỗi giao dịch chỉ được đặt tối đa 8 combo bắp nước.", ex.getMessage());
    }

    @Test
    void testCreateBooking_VoucherWarningBlock() {
        mockSystemConfigs();
        when(userService.findById(userId)).thenReturn(customerUser);
        when(showtimeService.findById(showtime.getId())).thenReturn(showtime);
        when(pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(showtimeSeatRepository.findByShowtimeAndIds(eq(showtime.getId()), anyList()))
                .thenReturn(List.of(showtimeSeat));
        when(pricingEngineService.calculateFinalSeatPrice(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PricingResult(new BigDecimal("80000"), BigDecimal.ZERO, "None"));
        when(pricingEngineService.calculateBestOrderPromotion(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, "None"));
        
        when(voucherService.validateForOrder(any(), any(), any())).thenThrow(new BadRequestException("Voucher has expired"));

        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getId().toString());
        request.setShowtimeSeatIds(List.of(showtimeSeat.getId().toString()));
        request.setVoucherCode("EXPIRED10");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(userId, request);
        });
        assertEquals("Mã giảm giá không hợp lệ hoặc đã hết hạn: Voucher has expired", ex.getMessage());
    }
}
