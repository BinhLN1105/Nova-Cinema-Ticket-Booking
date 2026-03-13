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
import com.cinema.ticket_booking.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class WalletServiceImpl implements WalletService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VnpayProperties vnpayProperties;

    @Override
    public PaymentResponse createTopUpUrl(UUID userId, BigDecimal amount, String returnUrlBase) {
        if (amount.compareTo(new BigDecimal("10000")) < 0) {
            throw new BadRequestException("Số tiền nạp tối thiểu là 10.000 VNĐ");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

        String txnRef = "TU" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(TransactionType.TOPUP_VNPAY)
                .status(TransactionStatus.PENDING)
                .referenceId(txnRef)
                .description("Nạp CinePoint qua VNPay")
                .build();
        
        transactionRepository.save(transaction);

        String returnUrl = returnUrlBase + "/api/v1/wallet/vnpay-return";
        String paymentUrl = buildVnpayUrl(amount, txnRef, returnUrl);

        return PaymentResponse.builder()
                .id(transaction.getId().toString())
                .status(com.cinema.ticket_booking.enums.PaymentStatus.valueOf(transaction.getStatus().name()))
                .method(com.cinema.ticket_booking.enums.PaymentMethod.VNPAY)
                .amount(amount)
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
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch nạp tiền: " + txnRef));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("VNPay callback cho giao dịch đã xử lý: {}", txnRef);
            return;
        }

        if ("00".equals(responseCode)) {
            transaction.setStatus(TransactionStatus.SUCCESS);

            // Cộng điểm: 1000 VNĐ = 1 Điểm
            long pointsToAdd = transaction.getAmount().divide(new BigDecimal("1000")).longValue();
            User user = transaction.getUser();
            user.setRewardPoints(user.getRewardPoints() + pointsToAdd);
            userRepository.save(user);

            log.info("Nạp CinePoint thành công: {}, +{} points", txnRef, pointsToAdd);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            log.warn("Nạp điểm thất bại: {} - ResponseCode: {}", txnRef, responseCode);
        }

        transactionRepository.save(transaction);
    }

    // ── VNPay helpers ─────────────────────────────────────────────────────

    private String buildVnpayUrl(BigDecimal amount, String txnRef, String returnUrl) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", "Nap CinePoint " + txnRef);
            params.put("vnp_OrderType", "190000"); // Mã danh mục cho phần nạp tiền
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String hashData = buildHashData(params);
            String signature = hmacSha512(hashData, vnpayProperties.getHashSecret());
            String queryString = buildQueryString(params) + "&vnp_SecureHash=" + signature;

            return vnpayProperties.getUrl() + "?" + queryString;
        } catch (Exception e) {
            throw new PaymentException("Không thể tạo URL nạp tiền VNPay");
        }
    }

    private boolean verifyVnpaySignature(Map<String, String> params, String receivedHash) {
        Map<String, String> filtered = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")) {
                filtered.put(k, v);
            }
        });
        try {
            String hashData = buildHashData(filtered);
            String computedHash = hmacSha512(hashData, vnpayProperties.getHashSecret());
            return computedHash.equals(receivedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
            }
        });
        return sb.toString();
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

    private String hmacSha512(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
