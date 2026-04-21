package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.CheckInResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.impl.ScanLogServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

        private final BookingService bookingService;
        private final ScanLogServiceImpl scanLogService;

        // POST /api/v1/bookings — tạo đơn đặt vé
        @PostMapping
        @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
        public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody BookingRequest request) {
                BookingResponse data = bookingService.createBooking(currentUser.getId(), request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(data,
                                                "Đặt vé thành công, vui lòng thanh toán trong 10 phút"));
        }

        // POST /api/v1/bookings/quote — lấy báo giá (không tạo đơn)
        @PostMapping("/quote")
        @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
        public ResponseEntity<ApiResponse<BookingResponse>> calculateQuote(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody BookingRequest request) {
                BookingResponse data = bookingService.calculateQuote(currentUser.getId(), request);
                return ResponseEntity.ok(ApiResponse.success(data, "Lấy báo giá thành công"));
        }

        // GET /api/v1/bookings/me?page=0&size=10 — lịch sử đặt vé của tôi
        @GetMapping("/me")
        public ResponseEntity<ApiResponse<PageResponse<BookingResponse.Summary>>> getMyBookings(
                        @AuthenticationPrincipal User currentUser,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                return ResponseEntity.ok(ApiResponse.success(
                                bookingService.getMyBookings(currentUser.getId(), pageable)));
        }

        // GET /api/v1/bookings/{id} — chi tiết đơn
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<BookingResponse>> getDetail(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(
                                bookingService.getDetail(currentUser.getId(), id)));
        }

        // POST /api/v1/bookings/{id}/cancel — Huỷ vé (Customer tự hủy hoặc Staff/Admin
        // hủy hộ)
        @PostMapping("/{id}/cancel")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> cancelBooking(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID id) {
                bookingService.cancelBooking(currentUser, id);
                return ResponseEntity.ok(
                                ApiResponse.success(null, "Huỷ vé thành công. Điểm CP đã được cộng vào tài khoản."));
        }

        // POST /api/v1/bookings/check-in [STAFF, ADMIN] — quét QR tại rạp
        @PostMapping("/check-in")
        @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
        public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
                        @AuthenticationPrincipal User currentUser,
                        @RequestParam String qrCode) {
                try {
                        CheckInResponse result = bookingService.checkIn(currentUser, qrCode);
                        return ResponseEntity.ok(ApiResponse.success(result, "Check-in thành công"));
                } catch (Exception e) {
                        // Ghi log scan thất bại vào ScanLog (transaction độc lập)
                        scanLogService.logFailedScan(currentUser, qrCode, e.getMessage());
                        throw e; // Re-throw để GlobalExceptionHandler xử lý
                }
        }

        // GET /api/v1/bookings/cancel-policy — Lấy chính sách hoàn vé
        @GetMapping("/cancel-policy")
        public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCancelPolicy(
                        @org.springframework.beans.factory.annotation.Autowired com.cinema.ticket_booking.service.SystemConfigService configService) {
                java.util.Map<String, Object> policy = new java.util.HashMap<>();
                policy.put("refundPercent", configService.getIntConfig("REFUND_PERCENT_CINEPOINT", 100));
                policy.put("minHoursBefore", configService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2));
                return ResponseEntity.ok(ApiResponse.success(policy));
        }
}
