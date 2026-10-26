package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.security.RateLimit;
import com.cinema.ticket_booking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

        private final PaymentService paymentService;

        @Value("${app.frontend.url}")
        private String frontendUrl;

        // POST /api/v1/payments — tạo URL thanh toán VNPay
        @PostMapping("/payments")
        @RateLimit(key = "create-payment", limit = 5, period = 60)
        public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody PaymentRequest request) {
                PaymentResponse data = paymentService.createPaymentUrl(currentUser.getId(), request);
                return ResponseEntity.ok(ApiResponse.success(data,
                                "Vui lòng mở paymentUrl để thanh toán qua VNPay"));
        }

        // POST /api/v1/payments/wallet/{bookingId}
        @PostMapping("/payments/wallet/{bookingId}")
        @RateLimit(key = "pay-wallet", limit = 5, period = 60)
        public ResponseEntity<ApiResponse<PaymentResponse>> payWithWallet(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID bookingId) {
                PaymentResponse data = paymentService.payWithWallet(currentUser.getId(), bookingId);
                return ResponseEntity.ok(ApiResponse.success(data, "Thanh toán bằng CinePoint thành công"));
        }

        // GET /api/v1/payments/vnpay/callback hoặc /api/v1/payment/vnpay-return
        @GetMapping({ "/payments/vnpay/callback", "/payment/vnpay-return" })
        public void vnpayCallback(@RequestParam Map<String, String> params, HttpServletResponse response)
                        throws IOException {
                String responseCode = params.get("vnp_ResponseCode");
                String txnRef = params.get("vnp_TxnRef");
                String source = params.get("source");

                String status = "failed";
                try {
                        paymentService.handleVnpayCallback(params);
                        if ("00".equals(responseCode)) {
                                status = "success";
                        }
                } catch (Exception e) {
                        log.error("[VNPay] Lỗi khi xử lý callback từ VNPay: txnRef={}, error={}", txnRef, e.getMessage(), e);
                        status = "failed";
                }

                // Sanitize responseCode chống Header Injection / CRLF
                String safeResponseCode = (responseCode != null && responseCode.matches("^[a-zA-Z0-9]+$"))
                                ? responseCode : "99";

                if ("mobile".equals(source)) {
                        // Mobile app → redirect về deep link để WebView chặn
                        String deepLink = "cinema://payment/result?vnp_ResponseCode=" + safeResponseCode + "&status="
                                        + status;

                        response.sendRedirect(deepLink);
                } else {
                        // Web frontend
                        String redirectUrl = frontendUrl + "/booking/result?status=" + status + "&vnp_ResponseCode="
                                        + safeResponseCode;

                        response.sendRedirect(redirectUrl);
                }
        }

        // GET /api/v1/payments/booking/{bookingId} — xem trạng thái thanh toán
        @GetMapping("/payments/booking/{bookingId}")
        public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID bookingId) {
                UUID userId = currentUser != null ? currentUser.getId() : null;
                return ResponseEntity.ok(ApiResponse.success(
                                paymentService.getByBookingId(userId, bookingId)));
        }
}
