package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.config.VnpayProperties;
import com.cinema.ticket_booking.util.VNPayUtils;
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
import com.cinema.ticket_booking.service.GiftCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GiftCardServiceImpl implements GiftCardService {

    private final GiftCardRepository giftCardRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final GiftCardMapper giftCardMapper;
    private final VnpayProperties vnpayProperties;

    @Override
    public PaymentResponse buyGiftCard(UUID userId, GiftCardRequest.Buy request) {
        if (request.getPrice().compareTo(new BigDecimal("50000")) < 0) {
            throw new BadRequestException("Mệnh giá thẻ tối thiểu là 50.000 VNĐ");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

        // Tạo Transaction chờ xử lý
        String txnRef = "GC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getPrice())
                .type(TransactionType.BUY_GIFT_CARD_VNPAY)
                .status(TransactionStatus.PENDING)
                .referenceId(txnRef)
                .description("Mua Gift Card Code CinePoint")
                .build();

        transactionRepository.save(transaction);

        String returnUrl = request.getReturnUrlBase() + "/api/v1/gift-cards/vnpay-return";
        String paymentUrl = buildVnpayUrl(request.getPrice(), txnRef, returnUrl);

        return PaymentResponse.builder()
                .id(transaction.getId().toString())
                .status(com.cinema.ticket_booking.enums.PaymentStatus.valueOf(transaction.getStatus().name()))
                .method(com.cinema.ticket_booking.enums.PaymentMethod.VNPAY)
                .amount(request.getPrice())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public void handleVnpayCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if (!verifyVnpaySignature(params, receivedHash)) {
            throw new PaymentException("Chữ ký VNPay không hợp lệ");
        }

        Transaction transaction = transactionRepository.findByReferenceId(txnRef)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch mua thẻ: " + txnRef));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("VNPay callback cho giao dịch tạo thẻ đã xử lý: {}", txnRef);
            return;
        }

        if ("00".equals(responseCode)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            
            // Giả định 1000đ = 1 CinePoint
            long pointValue = transaction.getAmount().divide(new BigDecimal("1000")).longValue();

            GiftCard giftCard = GiftCard.builder()
                    .code(generateUniqueGiftCardCode())
                    .price(transaction.getAmount())
                    .pointValue(pointValue)
                    .isRedeemed(false)
                    .boughtBy(transaction.getUser())
                    .expiresAt(LocalDateTime.now().plusMonths(6)) // Hạn 6 tháng
                    .build();

            giftCardRepository.save(giftCard);
            log.info("Tạo Gift Card {} mệnh giá {} ({}) thành công bởi txn {}", giftCard.getCode(), pointValue, transaction.getAmount(), txnRef);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            log.warn("Mua Gift Card thất bại: {} - ResponseCode: {}", txnRef, responseCode);
        }

        transactionRepository.save(transaction);
    }

    @Override
    public GiftCardResponse redeemGiftCard(UUID userId, GiftCardRequest.Redeem request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

        GiftCard giftCard = giftCardRepository.findByCode(request.getCode())
                .orElseThrow(() -> new BadRequestException("Mã thẻ không tồn tại"));

        if (giftCard.getIsRedeemed()) {
            throw new BadRequestException("Mã thẻ đã được sử dụng");
        }

        if (giftCard.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã thẻ đã hết hạn sử dụng");
        }

        // Đổi thẻ
        giftCard.setIsRedeemed(true);
        giftCard.setRedeemedBy(user);
        giftCard.setRedeemedAt(LocalDateTime.now());
        giftCardRepository.save(giftCard);

        // Nạp điểm
        user.setRewardPoints(user.getRewardPoints() + giftCard.getPointValue());
        userRepository.save(user);

        return giftCardMapper.toResponse(giftCard);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GiftCardResponse> getMyBoughtCards(UUID userId, Pageable pageable) {
        return PageResponse.of(giftCardRepository.findByBoughtByIdOrderByCreatedAtDesc(userId, pageable)
                .map(giftCardMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GiftCardResponse> getMyRedeemedCards(UUID userId, Pageable pageable) {
        return PageResponse.of(giftCardRepository.findByRedeemedByIdOrderByRedeemedAtDesc(userId, pageable)
                .map(giftCardMapper::toResponse));
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private String generateUniqueGiftCardCode() {
        // GC-XXXX-XXXX
        String part1 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String part2 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "GC-" + part1 + "-" + part2;
    }

    // ── VNPay Helpers (Similiar to Wallet) ───────────────────────────

    private String buildVnpayUrl(BigDecimal amount, String txnRef, String returnUrl) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", "Mua Gift Card CinePoint " + txnRef);
            params.put("vnp_OrderType", "190000"); // Mã danh mục
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String hashData = VNPayUtils.buildHashData(params);
            String signature = VNPayUtils.hmacSha512(hashData, vnpayProperties.getHashSecret());
            String queryString = buildQueryString(params) + "&vnp_SecureHash=" + signature;

            return vnpayProperties.getUrl() + "?" + queryString;
        } catch (Exception e) {
            throw new PaymentException("Không thể tạo URL nạp thẻ VNPay");
        }
    }

    private boolean verifyVnpaySignature(Map<String, String> params, String receivedHash) {
        Map<String, String> filtered = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType") && v != null && !v.isBlank()) {
                filtered.put(k, v);
            }
        });
        try {
            String hashData = VNPayUtils.buildHashData(filtered);
            String computedHash = VNPayUtils.hmacSha512(hashData, vnpayProperties.getHashSecret());
            return computedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildQueryString(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
            }
        }
        return sb.toString();
    }
}
