package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.PaymentStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ForbiddenException;
import com.cinema.ticket_booking.exception.PaymentException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PaymentMapper;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.PaymentRepository;
import com.cinema.ticket_booking.repository.TransactionRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.util.VNPayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Transactional
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "vnpayTmnCode", "TEST_TMN");
        ReflectionTestUtils.setField(paymentService, "vnpayHashSecret", "TEST_SECRET");
        ReflectionTestUtils.setField(paymentService, "vnpayUrl", "http://vnpay-test.com");
        ReflectionTestUtils.setField(paymentService, "vnpayReturnUrl", "http://return.com");
    }

    @Test
    void testCreatePaymentUrl_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .bookingCode("CODE123")
                .totalAmount(BigDecimal.valueOf(50000))
                .build();

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId().toString());

        Payment payment = Payment.builder().build();
        PaymentResponse response = PaymentResponse.builder().build();

        when(bookingService.findById(booking.getId())).thenReturn(booking);
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        try (MockedStatic<VNPayUtils> mocked = mockStatic(VNPayUtils.class)) {
            mocked.when(() -> VNPayUtils.buildPaymentUrl(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("http://vnpay.com/payment");

            PaymentResponse result = paymentService.createPaymentUrl(userId, request);
            assertNotNull(result);
            assertEquals("http://vnpay.com/payment", result.getPaymentUrl());
        }
    }

    @Test
    void testCreatePaymentUrl_Forbidden() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .build();

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId().toString());

        when(bookingService.findById(booking.getId())).thenReturn(booking);

        assertThrows(ForbiddenException.class, () -> paymentService.createPaymentUrl(userId, request));
    }

    @Test
    void testCreatePaymentUrl_NotPending() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(BookingStatus.PAID)
                .build();

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId().toString());

        when(bookingService.findById(booking.getId())).thenReturn(booking);

        assertThrows(BadRequestException.class, () -> paymentService.createPaymentUrl(userId, request));
    }

    @Test
    void testCreatePaymentUrl_Expired() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId().toString());

        when(bookingService.findById(booking.getId())).thenReturn(booking);

        assertThrows(BadRequestException.class, () -> paymentService.createPaymentUrl(userId, request));
    }

    @Test
    void testHandleVnpayCallback_Success() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "HASH");
        params.put("vnp_TxnRef", "TXN");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_BankCode", "NCB");

        Booking booking = Booking.builder().id(UUID.randomUUID()).build();
        Payment payment = Payment.builder().status(PaymentStatus.PENDING).booking(booking).build();
        PaymentResponse response = PaymentResponse.builder().build();

        try (MockedStatic<VNPayUtils> mocked = mockStatic(VNPayUtils.class)) {
            mocked.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);

            when(paymentRepository.findByVnpayTxnRef("TXN")).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            PaymentResponse result = paymentService.handleVnpayCallback(params);
            assertNotNull(result);
            assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
            verify(bookingService).confirmPaid(booking.getId());
        }
    }

    @Test
    void testHandleVnpayCallback_InvalidSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "HASH");

        try (MockedStatic<VNPayUtils> mocked = mockStatic(VNPayUtils.class)) {
            mocked.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(false);

            assertThrows(PaymentException.class, () -> paymentService.handleVnpayCallback(params));
        }
    }

    @Test
    void testPayWithWallet_Buyout_Success() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = User.builder().id(userId).rewardPoints(100L).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .user(user)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(50000))
                .bookingCode("CODE123")
                .build();

        PaymentResponse response = PaymentResponse.builder().build();

        when(bookingService.findById(bookingId)).thenReturn(booking);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        PaymentResponse result = paymentService.payWithWallet(userId, bookingId);
        assertNotNull(result);
        assertEquals(50L, user.getRewardPoints());
        verify(userRepository).save(user);
        verify(bookingService).confirmPaid(bookingId);
    }

    @Test
    void testPayWithWallet_Hybrid_Success() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = User.builder().id(userId).rewardPoints(20L).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .user(user)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(50000))
                .bookingCode("CODE123")
                .build();

        PaymentResponse response = PaymentResponse.builder().build();

        when(bookingService.findById(bookingId)).thenReturn(booking);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        PaymentResponse result = paymentService.payWithWallet(userId, bookingId);
        assertNotNull(result);
        assertEquals(0L, user.getRewardPoints());
        assertEquals(BigDecimal.valueOf(30000), result.getRemainingAmount());
    }

    @Test
    void testGetByBookingId_Success() {
        UUID bookingId = UUID.randomUUID();
        Payment payment = Payment.builder().build();
        PaymentResponse response = PaymentResponse.builder().build();

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.getByBookingId(bookingId);
        assertNotNull(result);
    }

    @Test
    void testGetByBookingId_NotFound() {
        UUID bookingId = UUID.randomUUID();
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getByBookingId(bookingId));
    }
}
