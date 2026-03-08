package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

        private final PaymentService paymentService;

        // POST /api/v1/payments — tạo URL thanh toán VNPay
        @PostMapping
        public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody PaymentRequest request) {
                PaymentResponse data = paymentService.createPaymentUrl(currentUser.getId(), request);
                return ResponseEntity.ok(ApiResponse.success(data,
                                "Vui lòng mở paymentUrl để thanh toán qua VNPay"));
        }

        // GET /api/v1/payments/vnpay/callback — VNPay redirect về sau thanh toán
        // Endpoint này PUBLIC (không cần token) vì VNPay gọi từ trình duyệt
        @GetMapping("/vnpay/callback")
        public ResponseEntity<ApiResponse<PaymentResponse>> vnpayCallback(
                        @RequestParam Map<String, String> params) {
                PaymentResponse data = paymentService.handleVnpayCallback(params);
                return ResponseEntity.ok(ApiResponse.success(data,
                                "00".equals(params.get("vnp_ResponseCode"))
                                                ? "Thanh toán thành công"
                                                : "Thanh toán thất bại"));
        }

        // GET /api/v1/payments/booking/{bookingId} — xem trạng thái thanh toán
        @GetMapping("/booking/{bookingId}")
        public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
                        @PathVariable UUID bookingId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentService.getByBookingId(bookingId)));
        }
}
