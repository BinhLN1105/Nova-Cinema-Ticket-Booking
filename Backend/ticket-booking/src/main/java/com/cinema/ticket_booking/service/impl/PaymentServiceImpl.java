package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Payment;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.PaymentStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.PaymentException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PaymentMapper;
import com.cinema.ticket_booking.model.Transaction;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.TransactionType;
import com.cinema.ticket_booking.enums.TransactionStatus;
import com.cinema.ticket_booking.repository.PaymentRepository;
import com.cinema.ticket_booking.repository.TransactionRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PaymentMapper paymentMapper;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Value("${vnpay.tmn-code}")
    private String vnpayTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpayHashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.return-url}")
    private String vnpayReturnUrl;

    // ── Tạo URL thanh toán VNPay ──────────────────────────────────────────

    @Override
    public PaymentResponse createPaymentUrl(UUID userId, PaymentRequest request) {
        Booking booking = bookingService.findById(UUID.fromString(request.getBookingId()));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đơn này");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Đơn đặt vé đã hết hạn, vui lòng đặt lại");
        }
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentException("Đơn hàng này đã được thanh toán");
            }
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .method(PaymentMethod.VNPAY)
                    .status(PaymentStatus.PENDING)
                    .build();
        }

        String txnRef = generateTxnRef(booking.getBookingCode());
        payment.setVnpayTxnRef(txnRef);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.VNPAY);
        paymentRepository.save(payment);

        // Tạo URL chuyển hướng đến cổng thanh toán VNPay
        String paymentUrl = buildVnpayUrl(booking.getTotalAmount(), txnRef,
                request.getReturnUrl() != null ? request.getReturnUrl() : vnpayReturnUrl);

        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setPaymentUrl(paymentUrl);
        return response;
    }

    // ── Xử lý callback từ VNPay ───────────────────────────────────────────

    /**
     * VNPay gọi về URL này sau khi user thanh toán.
     * Phải xác minh chữ ký trước khi cập nhật DB.
     */
    @Override
    public PaymentResponse handleVnpayCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        // 1. Xác minh chữ ký
        if (!verifyVnpaySignature(params, receivedHash)) {
            throw new PaymentException("Chữ ký VNPay không hợp lệ");
        }

        // 2. Tìm Payment theo txnRef
        Payment payment = paymentRepository.findByVnpayTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch", txnRef));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("VNPay callback cho giao dịch đã xử lý: {}", txnRef);
            return paymentMapper.toResponse(payment);
        }

        // 3. Cập nhật trạng thái
        payment.setVnpayResponseCode(responseCode);
        payment.setVnpayBankCode(params.get("vnp_BankCode"));

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Xác nhận booking + tạo QR + mark ghế BOOKED
            bookingService.confirmPaid(payment.getBooking().getId());
            log.info("Thanh toán thành công: {}", txnRef);
        } else {
            // Thanh toán thất bại → giải phóng ghế
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Thanh toán thất bại: {} - ResponseCode: {}", txnRef, responseCode);
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse payWithWallet(UUID userId, UUID bookingId) {
        Booking booking = bookingService.findById(bookingId);

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đơn này");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Đơn đặt vé đã hết hạn, vui lòng đặt lại");
        }

        long pointsNeeded = booking.getTotalAmount().divide(BigDecimal.valueOf(1000)).longValue();
        User user = booking.getUser();

        if (user.getRewardPoints() < pointsNeeded) {
            throw new BadRequestException("Số dư CinePoint không đủ để thanh toán. Vui lòng nạp thêm!");
        }

        // Trừ CinePoint
        user.setRewardPoints(user.getRewardPoints() - pointsNeeded);
        userRepository.save(user);

        // Lưu lịch sử giao dịch Wallet
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(booking.getTotalAmount())
                .type(TransactionType.PAYMENT_CREDIT)
                .status(TransactionStatus.SUCCESS)
                .referenceId(booking.getBookingCode())
                .description("Thanh toán vé bằng CinePoint")
                .build();
        transactionRepository.save(transaction);

        // Lưu lịch sử thanh toán hóa đơn
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .method(PaymentMethod.WALLET)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        bookingService.confirmPaid(booking.getId());
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByBookingId(UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment của booking", bookingId));
        return paymentMapper.toResponse(payment);
    }

    // ── VNPay helpers ─────────────────────────────────────────────────────

    private String buildVnpayUrl(BigDecimal amount, String txnRef, String returnUrl) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", vnpayTmnCode);
            // VNPay nhân thêm 100 (đơn vị VND * 100)
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", "Thanh toan ve xem phim " + txnRef);
            params.put("vnp_OrderType", "250000");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String hashData = buildHashData(params);
            String signature = hmacSha512(hashData, vnpayHashSecret);
            String queryString = buildQueryString(params) + "&vnp_SecureHash=" + signature;

            return vnpayUrl + "?" + queryString;
        } catch (Exception e) {
            throw new PaymentException("Không thể tạo URL thanh toán VNPay");
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
            String computedHash = hmacSha512(hashData, vnpayHashSecret);
            return computedHash.equals(receivedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0)
                    sb.append('&');
                sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
            }
        });
        return sb.toString();
    }

    private String buildQueryString(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                if (sb.length() > 0)
                    sb.append('&');
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
        for (byte b : raw)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String generateTxnRef(String bookingCode) {
        return bookingCode + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }
}
