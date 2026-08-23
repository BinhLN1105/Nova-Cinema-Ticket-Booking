package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.enums.*;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationCampaignRepository notificationCampaignRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    private User testUser;
    private Booking testBooking;
    private ShowtimeSeat testShowtimeSeat;
    private BookingItem testBookingItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .rewardPoints(100L)
                .build();

        testBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK123456")
                .status(BookingStatus.PENDING)
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        testShowtimeSeat = ShowtimeSeat.builder()
                .id(UUID.randomUUID())
                .status(SeatStatus.LOCKED)
                .lockedBy(testUser)
                .lockedUntil(LocalDateTime.now().minusMinutes(5))
                .build();

        testBookingItem = BookingItem.builder()
                .id(UUID.randomUUID())
                .booking(testBooking)
                .showtimeSeat(testShowtimeSeat)
                .build();
    }

    @Test
    void testReleaseExpiredSeatLocks_Success() {
        when(showtimeSeatRepository.releaseExpiredLocks(any(LocalDateTime.class))).thenReturn(5);

        assertDoesNotThrow(() -> schedulerService.releaseExpiredSeatLocks());
        verify(showtimeSeatRepository, times(1)).releaseExpiredLocks(any(LocalDateTime.class));
    }

    @Test
    void testReleaseExpiredSeatLocks_ExceptionHandled() {
        when(showtimeSeatRepository.releaseExpiredLocks(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("DB Error"));

        assertDoesNotThrow(() -> schedulerService.releaseExpiredSeatLocks());
    }

    @Test
    void testExpireOverdueBookings_WithCinePointRefund() {
        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(testBooking));
        when(bookingItemRepository.findByBookingIdWithSeat(testBooking.getId()))
                .thenReturn(List.of(testBookingItem));

        Transaction creditTx = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .amount(BigDecimal.valueOf(50000))
                .type(TransactionType.PAYMENT_CREDIT)
                .status(TransactionStatus.SUCCESS)
                .referenceId(testBooking.getBookingCode())
                .build();

        when(transactionRepository.findByReferenceIdAndType(testBooking.getBookingCode(), TransactionType.PAYMENT_CREDIT))
                .thenReturn(List.of(creditTx));
        when(transactionRepository.existsByReferenceIdAndType(testBooking.getBookingCode(), TransactionType.REFUND))
                .thenReturn(false);
        when(bookingRepository.expireOverdueBookings(any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> schedulerService.expireOverdueBookings());

        verify(showtimeSeatRepository, times(1)).save(argThat(ss -> ss.getStatus() == SeatStatus.AVAILABLE));
        verify(userRepository, times(1)).save(argThat(u -> u.getRewardPoints() == 150L));
        verify(transactionRepository, times(1)).save(argThat(tx -> tx.getType() == TransactionType.REFUND));
        verify(bookingRepository, times(1)).expireOverdueBookings(any(LocalDateTime.class));
    }

    @Test
    void testExpireOverdueBookings_AlreadyRefunded_SkipRefund() {
        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(testBooking));
        when(bookingItemRepository.findByBookingIdWithSeat(testBooking.getId()))
                .thenReturn(List.of(testBookingItem));

        Transaction creditTx = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50000))
                .type(TransactionType.PAYMENT_CREDIT)
                .build();

        when(transactionRepository.findByReferenceIdAndType(testBooking.getBookingCode(), TransactionType.PAYMENT_CREDIT))
                .thenReturn(List.of(creditTx));
        when(transactionRepository.existsByReferenceIdAndType(testBooking.getBookingCode(), TransactionType.REFUND))
                .thenReturn(true);
        when(bookingRepository.expireOverdueBookings(any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> schedulerService.expireOverdueBookings());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testExpireOverdueBookings_ExceptionHandled() {
        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("DB Connection Timeout"));

        assertDoesNotThrow(() -> schedulerService.expireOverdueBookings());
    }

    @Test
    void testCleanExpiredRefreshTokens_Success() {
        doNothing().when(refreshTokenRepository).deleteAllExpiredBefore(any(LocalDateTime.class));

        assertDoesNotThrow(() -> schedulerService.cleanExpiredRefreshTokens());
        verify(refreshTokenRepository, times(1)).deleteAllExpiredBefore(any(LocalDateTime.class));
    }

    @Test
    void testSendShowtimeReminders_Success() {
        Movie movie = Movie.builder().title("Inception").build();
        Showtime showtime = Showtime.builder().movie(movie).build();
        Booking paidBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .showtime(showtime)
                .status(BookingStatus.PAID)
                .build();

        when(bookingRepository.findUpcomingBookings(eq(BookingStatus.PAID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(paidBooking));

        assertDoesNotThrow(() -> schedulerService.sendShowtimeReminders());
        verify(notificationService, times(1)).sendPromotion(
                eq(testUser), anyString(), contains("Inception"), eq(paidBooking.getId()));
    }

    @Test
    void testSendShowtimeReminders_NotificationException_ContinuesLoop() {
        Movie movie = Movie.builder().title("Inception").build();
        Showtime showtime = Showtime.builder().movie(movie).build();
        Booking paidBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .showtime(showtime)
                .status(BookingStatus.PAID)
                .build();

        when(bookingRepository.findUpcomingBookings(eq(BookingStatus.PAID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(paidBooking));
        doThrow(new RuntimeException("FCM Fail")).when(notificationService)
                .sendPromotion(any(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> schedulerService.sendShowtimeReminders());
    }

    @Test
    void testProcessScheduledCampaigns_Success() {
        NotificationCampaign campaign = NotificationCampaign.builder()
                .id(UUID.randomUUID())
                .title("Special Discount")
                .body("Get 20% off today")
                .type(NotificationType.PROMOTION)
                .status(CampaignStatus.PENDING)
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(notificationCampaignRepository.findByStatusAndScheduledAtBefore(eq(CampaignStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(campaign));

        assertDoesNotThrow(() -> schedulerService.processScheduledCampaigns());

        verify(notificationService, times(1)).broadcastGlobalNotification(
                eq("Special Discount"), eq("Get 20% off today"), eq(NotificationType.PROMOTION), any(), any());
        verify(notificationCampaignRepository, times(1)).save(argThat(c -> c.getStatus() == CampaignStatus.SENT));
    }

    @Test
    void testProcessScheduledCampaigns_EmptyList_ReturnsEarly() {
        when(notificationCampaignRepository.findByStatusAndScheduledAtBefore(eq(CampaignStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> schedulerService.processScheduledCampaigns());
        verify(notificationService, never()).broadcastGlobalNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testAutoUpdateMovieLifecycle_Success() {
        when(movieService.autoUpdateMovieStatuses()).thenReturn(3);

        assertDoesNotThrow(() -> schedulerService.autoUpdateMovieLifecycle());
        verify(movieService, times(1)).autoUpdateMovieStatuses();
    }

    @Test
    void testAutoUpdateMovieLifecycle_ExceptionHandled() {
        when(movieService.autoUpdateMovieStatuses()).thenThrow(new RuntimeException("Error"));

        assertDoesNotThrow(() -> schedulerService.autoUpdateMovieLifecycle());
    }
}
