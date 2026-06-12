package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.config.VnpayProperties;
import com.cinema.ticket_booking.util.VNPayUtils;
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

// import javax.crypto.Mac;
// import javax.crypto.spec.SecretKeySpec;
// import java.net.URLEncoder;
// import java.nio.charset.StandardCharsets;
// import java.util.TreeMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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

        String txnRef = "TU" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

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

        if (!VNPayUtils.verifySignature(params, receivedHash, vnpayProperties.getHashSecret())) {
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
            long currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0L;
            user.setRewardPoints(currentPoints + pointsToAdd);
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
            return VNPayUtils.buildPaymentUrl(
                    vnpayProperties.getUrl(),
                    vnpayProperties.getTmnCode(),
                    vnpayProperties.getHashSecret(),
                    amount,
                    txnRef,
                    "Nap CinePoint " + txnRef,
                    returnUrl);
        } catch (Exception e) {
            throw new PaymentException("Không thể tạo URL nạp tiền VNPay");
        }
    }
}
