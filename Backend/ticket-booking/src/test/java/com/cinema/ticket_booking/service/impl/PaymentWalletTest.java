package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.PaymentStatus;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PaymentMapper;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Payment;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Additional payment boundary tests required by Sprint 3; all collaborators are Mockito mocks. */
@ExtendWith(MockitoExtension.class)
@Transactional
class PaymentWalletTest {
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingService bookingService;
    @Mock private PaymentMapper paymentMapper;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "vnpayHashSecret", "test-secret");
    }

    @Test
    void vnpayFailedCallbackMarksPaymentFailedWithoutConfirmingBooking() {
        Booking booking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.PENDING).build();
        Payment payment = Payment.builder().booking(booking).status(PaymentStatus.PENDING).build();
        Map<String, String> params = callback("24");
        when(paymentRepository.findByVnpayTxnRef("TXN-1")).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(PaymentResponse.builder().build());

        try (MockedStatic<VNPayUtils> vnpay = mockStatic(VNPayUtils.class)) {
            vnpay.when(() -> VNPayUtils.verifySignature(anyMap(), anyString(), anyString())).thenReturn(true);
            paymentService.handleVnpayCallback(params);
        }

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(bookingService, never()).confirmPaid(any());
        verify(paymentRepository).save(payment);
    }

    @Test
    void vnpayDuplicateCallbackIsIdempotent() {
        Payment payment = Payment.builder().status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findByVnpayTxnRef("TXN-1")).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(PaymentResponse.builder().build());

        try (MockedStatic<VNPayUtils> vnpay = mockStatic(VNPayUtils.class)) {
            vnpay.when(() -> VNPayUtils.verifySignature(anyMap(), anyString(), anyString())).thenReturn(true);
            assertDoesNotThrow(() -> paymentService.handleVnpayCallback(callback("00")));
        }

        verify(paymentRepository, never()).save(payment);
        verify(bookingService, never()).confirmPaid(any());
    }

    @Test
    void vnpayCallbackRejectsUnknownTransaction() {
        when(paymentRepository.findByVnpayTxnRef("TXN-1")).thenReturn(Optional.empty());
        try (MockedStatic<VNPayUtils> vnpay = mockStatic(VNPayUtils.class)) {
            vnpay.when(() -> VNPayUtils.verifySignature(anyMap(), anyString(), anyString())).thenReturn(true);
            assertThrows(ResourceNotFoundException.class, () -> paymentService.handleVnpayCallback(callback("00")));
        }
    }

    private Map<String, String> callback(String responseCode) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "signature");
        params.put("vnp_TxnRef", "TXN-1");
        params.put("vnp_ResponseCode", responseCode);
        return params;
    }
}
