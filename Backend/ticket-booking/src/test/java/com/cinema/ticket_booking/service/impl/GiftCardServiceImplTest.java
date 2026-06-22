package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.config.VnpayProperties;
import com.cinema.ticket_booking.dto.request.GiftCardRequest;
import com.cinema.ticket_booking.dto.response.GiftCardResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.enums.TransactionStatus;
import com.cinema.ticket_booking.enums.TransactionType;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.PaymentException;
import com.cinema.ticket_booking.mapper.GiftCardMapper;
import com.cinema.ticket_booking.model.GiftCard;
import com.cinema.ticket_booking.model.Transaction;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.GiftCardRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftCardServiceImplTest {

    @Mock
    private GiftCardRepository giftCardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GiftCardMapper giftCardMapper;

    @Mock
    private VnpayProperties vnpayProperties;

    @InjectMocks
    private GiftCardServiceImpl giftCardService;

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
    void testBuyGiftCard_PriceTooLow() {
        GiftCardRequest.Buy request = new GiftCardRequest.Buy();
        request.setPrice(new BigDecimal("40000"));

        assertThrows(BadRequestException.class, () -> giftCardService.buyGiftCard(userId, request));
    }

    @Test
    void testBuyGiftCard_UserNotFound() {
        GiftCardRequest.Buy request = new GiftCardRequest.Buy();
        request.setPrice(new BigDecimal("50000"));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> giftCardService.buyGiftCard(userId, request));
    }

    @Test
    void testBuyGiftCard_Success() {
        GiftCardRequest.Buy request = new GiftCardRequest.Buy();
        request.setPrice(new BigDecimal("50000"));
        request.setReturnUrlBase("http://localhost:8080");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vnpayProperties.getUrl()).thenReturn("https://sandbox.vnpayment.vn");
        when(vnpayProperties.getTmnCode()).thenReturn("DEMO");
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        PaymentResponse response = giftCardService.buyGiftCard(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000"), response.getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testHandleVnpayCallback_InvalidSignature() {
        when(vnpayProperties.getHashSecret()).thenReturn("SECRET");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "invalid");

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(false);
            assertThrows(PaymentException.class, () -> giftCardService.handleVnpayCallback(params));
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

            assertThrows(BadRequestException.class, () -> giftCardService.handleVnpayCallback(params));
        }
    }

    @Test
    void testHandleVnpayCallback_AlreadyProcessed() {
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

            assertDoesNotThrow(() -> giftCardService.handleVnpayCallback(params));
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
                .amount(new BigDecimal("100000"))
                .user(user)
                .build();

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.of(transaction));

            giftCardService.handleVnpayCallback(params);

            assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
            verify(giftCardRepository, times(1)).save(any(GiftCard.class));
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
                .build();

        try (MockedStatic<VNPayUtils> mockedVNPayUtils = mockStatic(VNPayUtils.class)) {
            mockedVNPayUtils.when(() -> VNPayUtils.verifySignature(any(), any(), any())).thenReturn(true);
            when(transactionRepository.findByReferenceId("TXN123")).thenReturn(Optional.of(transaction));

            giftCardService.handleVnpayCallback(params);

            assertEquals(TransactionStatus.FAILED, transaction.getStatus());
            verify(giftCardRepository, never()).save(any());
            verify(transactionRepository, times(1)).save(transaction);
        }
    }

    @Test
    void testRedeemGiftCard_UserNotFound() {
        GiftCardRequest.Redeem request = new GiftCardRequest.Redeem();
        request.setCode("GC-1234-5678");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> giftCardService.redeemGiftCard(userId, request));
    }

    @Test
    void testRedeemGiftCard_CardNotFound() {
        GiftCardRequest.Redeem request = new GiftCardRequest.Redeem();
        request.setCode("GC-1234-5678");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(giftCardRepository.findByCode("GC-1234-5678")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> giftCardService.redeemGiftCard(userId, request));
    }

    @Test
    void testRedeemGiftCard_AlreadyRedeemed() {
        GiftCardRequest.Redeem request = new GiftCardRequest.Redeem();
        request.setCode("GC-1234-5678");

        GiftCard giftCard = GiftCard.builder()
                .code("GC-1234-5678")
                .isRedeemed(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(giftCardRepository.findByCode("GC-1234-5678")).thenReturn(Optional.of(giftCard));

        assertThrows(BadRequestException.class, () -> giftCardService.redeemGiftCard(userId, request));
    }

    @Test
    void testRedeemGiftCard_Expired() {
        GiftCardRequest.Redeem request = new GiftCardRequest.Redeem();
        request.setCode("GC-1234-5678");

        GiftCard giftCard = GiftCard.builder()
                .code("GC-1234-5678")
                .isRedeemed(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(giftCardRepository.findByCode("GC-1234-5678")).thenReturn(Optional.of(giftCard));

        assertThrows(BadRequestException.class, () -> giftCardService.redeemGiftCard(userId, request));
    }

    @Test
    void testRedeemGiftCard_Success() {
        GiftCardRequest.Redeem request = new GiftCardRequest.Redeem();
        request.setCode("GC-1234-5678");

        GiftCard giftCard = GiftCard.builder()
                .code("GC-1234-5678")
                .isRedeemed(false)
                .expiresAt(LocalDateTime.now().plusMonths(6))
                .pointValue(50L)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(giftCardRepository.findByCode("GC-1234-5678")).thenReturn(Optional.of(giftCard));

        GiftCardResponse expectedResponse = new GiftCardResponse();
        expectedResponse.setCode("GC-1234-5678");
        when(giftCardMapper.toResponse(giftCard)).thenReturn(expectedResponse);

        GiftCardResponse result = giftCardService.redeemGiftCard(userId, request);

        assertNotNull(result);
        assertEquals("GC-1234-5678", result.getCode());
        assertTrue(giftCard.getIsRedeemed());
        assertEquals(150L, user.getRewardPoints()); // 100 + 50
        verify(giftCardRepository, times(1)).save(giftCard);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testGetMyBoughtCards() {
        Pageable pageable = PageRequest.of(0, 10);
        GiftCard giftCard = GiftCard.builder().code("GC-1234").build();
        Page<GiftCard> page = new PageImpl<>(List.of(giftCard));

        when(giftCardRepository.findByBoughtByIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);
        
        GiftCardResponse response = new GiftCardResponse();
        response.setCode("GC-1234");
        when(giftCardMapper.toResponse(giftCard)).thenReturn(response);

        PageResponse<GiftCardResponse> result = giftCardService.getMyBoughtCards(userId, pageable);
        assertEquals(1, result.getContent().size());
        assertEquals("GC-1234", result.getContent().get(0).getCode());
    }

    @Test
    void testGetMyRedeemedCards() {
        Pageable pageable = PageRequest.of(0, 10);
        GiftCard giftCard = GiftCard.builder().code("GC-5678").build();
        Page<GiftCard> page = new PageImpl<>(List.of(giftCard));

        when(giftCardRepository.findByRedeemedByIdOrderByRedeemedAtDesc(userId, pageable)).thenReturn(page);

        GiftCardResponse response = new GiftCardResponse();
        response.setCode("GC-5678");
        when(giftCardMapper.toResponse(giftCard)).thenReturn(response);

        PageResponse<GiftCardResponse> result = giftCardService.getMyRedeemedCards(userId, pageable);
        assertEquals(1, result.getContent().size());
        assertEquals("GC-5678", result.getContent().get(0).getCode());
    }
}
