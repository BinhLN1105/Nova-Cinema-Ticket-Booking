package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.config.VnpayProperties;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.enums.TransactionStatus;
import com.cinema.ticket_booking.enums.TransactionType;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.PaymentException;
import com.cinema.ticket_booking.model.Transaction;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.TransactionRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.util.VNPayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Transactional
class WalletServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VnpayProperties vnpayProperties;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .rewardPoints(100L)
                .build();
    }

    @Test
    void testCreateTopUpUrl_AmountTooLow() {
        assertThrows(BadRequestException.class, () -> {
            walletService.createTopUpUrl(userId, new BigDecimal("5000"), "http://localhost:8080");
        });
    }

    @Test
    void testCreateTopUpUrl_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            walletService.createTopUpUrl(userId, new BigDecimal("20000"), "http://localhost:8080");
        });
    }

    @Test
    void testCreateTopUpUrl_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vnpayProperties.getUrl()).thenReturn("https://sandbox.vnpayment.vn");
        when(vnpayProperties.getTmnCode()).thenReturn("DEMO");
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        PaymentResponse response = walletService.createTopUpUrl(userId, new BigDecimal("20000"), "http://localhost:8080");

        assertNotNull(response);
        assertEquals(new BigDecimal("20000"), response.getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testHandleVnpayCallback_InvalidSignature() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "invalid");

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(false);
            assertThrows(PaymentException.class, () -> walletService.handleVnpayCallback(params));
        }
    }

    @Test
    void testHandleVnpayCallback_TransactionNotFound() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "valid");
        params.put("vnp_TxnRef", "TXN123");

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class, () -> walletService.handleVnpayCallback(params));
        }
    }

    @Test
    void testHandleVnpayCallback_TransactionAlreadyProcessed() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "valid");
        params.put("vnp_TxnRef", "TXN123");

        Transaction transaction = Transaction.builder()
                .status(TransactionStatus.SUCCESS)
                .build();

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.of(transaction));

            assertDoesNotThrow(() -> walletService.handleVnpayCallback(params));
            verify(transactionRepository, never()).save(any());
        }
    }

    @Test
    void testHandleVnpayCallback_Success() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "valid");
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_ResponseCode", "00");

        Transaction transaction = Transaction.builder()
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("100000")) // 100 points
                .user(user)
                .build();

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.of(transaction));

            walletService.handleVnpayCallback(params);

            assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
            assertEquals(200L, user.getRewardPoints()); // 100 + 100
            verify(userRepository, times(1)).save(user);
            verify(transactionRepository, times(1)).save(transaction);
        }
    }

    @Test
    void testHandleVnpayCallback_FailedResponse() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "valid");
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_ResponseCode", "99");

        Transaction transaction = Transaction.builder()
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("100000"))
                .user(user)
                .build();

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.of(transaction));

            walletService.handleVnpayCallback(params);

            assertEquals(TransactionStatus.FAILED, transaction.getStatus());
            assertEquals(100L, user.getRewardPoints()); // Points remain unchanged
            verify(userRepository, never()).save(user);
            verify(transactionRepository, times(1)).save(transaction);
        }
    }
}
