package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

        private final PaymentService paymentService;

        @Value("${app.frontend.url}")
        private String frontendUrl;

        // POST /api/v1/payments — tạo URL thanh toán VNPay
        @PostMapping("/payments")
        public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody PaymentRequest request) {
                PaymentResponse data = paymentService.createPaymentUrl(currentUser.getId(), request);
                return ResponseEntity.ok(ApiResponse.success(data,
                                "Vui lòng mở paymentUrl để thanh toán qua VNPay"));
        }

        // POST /api/v1/payments/wallet/{bookingId}
        @PostMapping("/payments/wallet/{bookingId}")
        public ResponseEntity<ApiResponse<PaymentResponse>> payWithWallet(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID bookingId) {
                PaymentResponse data = paymentService.payWithWallet(currentUser.getId(), bookingId);
                return ResponseEntity.ok(ApiResponse.success(data, "Thanh toán bằng CinePoint thành công"));
        }

        // GET /api/v1/payments/vnpay/callback hoặc /api/v1/payment/vnpay-return
        // Endpoint này PUBLIC (không cần token) vì VNPay gọi từ trình duyệt
        @GetMapping({"/payments/vnpay/callback", "/payment/vnpay-return"})
        public void vnpayCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
                paymentService.handleVnpayCallback(params);
                String responseCode = params.get("vnp_ResponseCode");
                String source = params.get("source");

                if ("mobile".equals(source)) {
                        // Mobile app → redirect về deep link để WebView chặn
                        String deepLink = "cinema://payment/result?vnp_ResponseCode=" + responseCode;
                        response.sendRedirect(deepLink);
                } else {
                        // Web frontend
                        String status = "00".equals(responseCode) ? "success" : "failed";
                        response.sendRedirect(frontendUrl + "/booking/result?status=" + status);
                }
        }

        // GET /api/v1/payments/booking/{bookingId} — xem trạng thái thanh toán
        @GetMapping("/payments/booking/{bookingId}")
        public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
                        @PathVariable UUID bookingId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentService.getByBookingId(bookingId)));
        }
}
