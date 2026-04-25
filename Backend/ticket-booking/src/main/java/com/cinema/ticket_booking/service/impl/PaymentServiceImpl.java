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
import java.math.RoundingMode;

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
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (!verifyVnpaySignature(params, vnp_SecureHash)) {
            throw new PaymentException("Chữ ký VNPay không hợp lệ (Checksum failed)");
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

    // Ngưỡng tối thiểu của cổng thanh toán (10.000đ)
    private static final BigDecimal MIN_GATEWAY_AMOUNT = BigDecimal.valueOf(10_000);
    // Tỷ lệ quy đổi: 1 CP = 1.000đ
    private static final BigDecimal CP_RATE = BigDecimal.valueOf(1_000);

    /**
     * Thuật toán tính CinePoint:
     * 1. maxCpApplicable = floor(totalAmount / 1000) → số CP tối đa đơn này cho
     * phép dùng
     * 2. actualCp = min(maxCpApplicable, userBalance)
     * 3. pointDiscount = actualCp * 1000
     * 4. remaining = totalAmount - pointDiscount
     * 5. Nếu 0 < remaining < 10.000 → giảm actualCp để remaining = 0 (full CP) hoặc
     * >= 10.000
     */
    @Override
    public PaymentResponse payWithWallet(UUID userId, UUID bookingId) {
        Booking booking = bookingService.findById(bookingId);

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đơn này");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Đơn đặt vé đã hết hạn, vui lòng đặt lại");
        }

        BigDecimal totalAmount = booking.getTotalAmount();
        User user = booking.getUser();
        long userBalance = user.getRewardPoints() != null ? user.getRewardPoints() : 0L;

        // Kiểm tra xem có đủ điểm "Mua đứt" (Buyout) không
        // Nếu dùng Ceil(totalAmount/1000) mà vẫn đủ số dư -> Cho phép thanh toán 100%
        long cpNeededToCoverAll = totalAmount
                .divide(CP_RATE, 0, RoundingMode.CEILING)
                .longValue();

        long actualCp;
        BigDecimal pointDiscount;
        BigDecimal remaining;

        if (userBalance >= cpNeededToCoverAll && cpNeededToCoverAll > 0) {
            // Trường hợp 1: Đủ điểm trả HẾT (Buyout)
            actualCp = cpNeededToCoverAll;
            pointDiscount = totalAmount; // Coi như giảm 100%
            remaining = BigDecimal.ZERO;
            log.info("Người dùng đủ điểm mua đứt: {} CP cho đơn hàng {}đ", actualCp, totalAmount);
        } else {
            // Trường hợp 2: Không đủ trả hết -> Thanh toán lai (Hybrid)
            // Bước 1: Số CP tối đa đơn này cho phép (floor division)
            long maxCpApplicable = totalAmount
                    .divideToIntegralValue(CP_RATE)
                    .longValue();

            // Bước 2: Không dùng nhiều hơn số dư ví
            actualCp = Math.min(maxCpApplicable, userBalance);

            if (actualCp <= 0) {
                throw new BadRequestException("Số dư CinePoint không đủ để thanh toán. Vui lòng nạp thêm!");
            }

            // Bước 3: Tính số tiền giảm từ CP
            pointDiscount = BigDecimal.valueOf(actualCp).multiply(CP_RATE);

            // Bước 4: Số tiền còn lại phải thanh toán qua cổng
            remaining = totalAmount.subtract(pointDiscount);

            // Bước 5: Kiểm tra ngưỡng tối thiểu cổng thanh toán (10.000đ)
            // Nếu remaining > 0 nhưng < 10.000đ → phải điều chỉnh CP để không gây lỗi cổng
            if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(MIN_GATEWAY_AMOUNT) < 0) {
                // Giảm CP xuống sao cho remaining = totalAmount - newCp*1000 >= 10.000đ
                long adjustedCp = totalAmount.subtract(MIN_GATEWAY_AMOUNT)
                        .divideToIntegralValue(CP_RATE)
                        .longValue();
                adjustedCp = Math.max(0, Math.min(adjustedCp, userBalance));
                actualCp = adjustedCp;
                pointDiscount = BigDecimal.valueOf(actualCp).multiply(CP_RATE);
                remaining = totalAmount.subtract(pointDiscount);
                log.info("CinePoint điều chỉnh xuống {} CP để phần dư {}đ >= 10.000đ", actualCp, remaining);
            }
        }

        // Nếu full CP (remaining = 0) → hoàn tất toàn bộ bằng CP
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            // Trừ CP, đánh dấu booking PAID ngay
            user.setRewardPoints(userBalance - actualCp);
            userRepository.save(user);

            Transaction transaction = Transaction.builder()
                    .user(user)
                    .amount(totalAmount)
                    .type(TransactionType.PAYMENT_CREDIT)
                    .status(TransactionStatus.SUCCESS)
                    .referenceId(booking.getBookingCode())
                    .description("Thanh toán toàn bộ bằng CinePoint (" + actualCp + " CP)")
                    .build();
            transactionRepository.save(transaction);

            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(pointDiscount)
                    .method(PaymentMethod.WALLET)
                    .status(PaymentStatus.SUCCESS)
                    .paidAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);

            bookingService.confirmPaid(booking.getId());
            log.info("Thanh toán full CinePoint: {} CP = {}đ, booking {}", actualCp, pointDiscount,
                    booking.getBookingCode());
            PaymentResponse response = paymentMapper.toResponse(payment);
            response.setPointsUsed(actualCp);
            response.setPointDiscount(pointDiscount);
            response.setRemainingAmount(BigDecimal.ZERO);
            return response;

        } else {
            // Thanh toán lai (CP + cổng thanh toán)
            // Bước này: trừ CP ngay, trả về remaining để FE tiếp tục qua VNPay/MoMo
            user.setRewardPoints(userBalance - actualCp);
            userRepository.save(user);

            Transaction transaction = Transaction.builder()
                    .user(user)
                    .amount(pointDiscount)
                    .type(TransactionType.PAYMENT_CREDIT)
                    .status(TransactionStatus.SUCCESS)
                    .referenceId(booking.getBookingCode())
                    .description("Dùng " + actualCp + " CP — còn lại " + remaining + "đ thanh toán qua cổng")
                    .build();
            transactionRepository.save(transaction);

            // Cập nhật lại totalAmount của booking = phần còn lại cần thanh toán qua cổng
            booking.setTotalAmount(remaining);
            // Ghi nhận đã dùng CP một phần vào discountAmount
            BigDecimal existingDiscount = booking.getDiscountAmount() != null ? booking.getDiscountAmount()
                    : BigDecimal.ZERO;
            booking.setDiscountAmount(existingDiscount.add(pointDiscount));

            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(remaining)
                    .method(PaymentMethod.WALLET)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);

            PaymentResponse response = paymentMapper.toResponse(payment);
            response.setPointsUsed(actualCp);
            response.setPointDiscount(pointDiscount);
            response.setRemainingAmount(remaining);
            log.info("Thanh toán lai: {} CP = {}đ, còn lại {}đ, booking {}", actualCp, pointDiscount, remaining,
                    booking.getBookingCode());
            return response;
        }
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
        // Tạo một bản sao để tránh làm hỏng map gốc nếu cần dùng sau này
        Map<String, String> vnp_Params = new HashMap<>(params);

        // Bắt buộc loại bỏ SecureHash trước khi tính toán
        vnp_Params.remove("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");

        // Lọc các tham số bắt đầu bằng vnp_ và sắp xếp
        Map<String, String> filtered = new TreeMap<>();
        vnp_Params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && v != null && !v.isBlank()) {
                filtered.put(k, v);
            }
        });

        try {
            // VNPay 2.1.0: Dùng chuỗi Raw (KHÔNG URL Encode) để verify
            String hashData = buildRawHashData(filtered);
            String computedHash = hmacSha512(hashData, vnpayHashSecret);
            return computedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception e) {
            log.error("Lỗi khi xác thực chữ ký VNPay: {}", e.getMessage());
            return false;
        }
    }

    private String buildRawHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Bước 1: Build Key=Value
                hashData.append(fieldName).append("=");

                // Bước 2: Encode Value theo chuẩn VNPay (Space -> +)
                // URLEncoder.encode() chuyển Space thành +
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
                hashData.append(encodedValue);

                hashData.append("&");
            }
        }
        // Xóa dấu '&' cuối cùng
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        return hashData.toString();
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
