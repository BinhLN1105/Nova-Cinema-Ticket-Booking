package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.CheckInResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.BookingService;

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

        // POST /api/v1/bookings — tạo đơn đặt vé
        @PostMapping
        @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
        public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
                        @AuthenticationPrincipal User currentUser,
                        @Valid @RequestBody BookingRequest request) {
                BookingResponse data = bookingService.createBooking(currentUser.getId(), request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(data,
                                                "Đặt vé thành công, vui lòng thanh toán trong 10 phút"));
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

        // POST /api/v1/bookings/{id}/cancel-request — Yêu cầu huỷ đơn vé đã thanh toán
        @PostMapping("/{id}/cancel-request")
        public ResponseEntity<ApiResponse<Void>> requestCancelBooking(
                        @AuthenticationPrincipal User currentUser,
                        @PathVariable UUID id) {
                bookingService.requestCancelBooking(currentUser.getId(), id);
                return ResponseEntity.ok(ApiResponse.success(null, "Yêu cầu huỷ vé thành công. Vui lòng kiểm tra email để xác nhận."));
        }

        // POST /api/v1/bookings/cancel-confirm — Xác nhận huỷ vé qua token email
        @PostMapping("/cancel-confirm")
        public ResponseEntity<ApiResponse<Void>> confirmCancelBooking(
                        @RequestParam String token,
                        @RequestParam UUID bookingId) {
                bookingService.confirmCancelBooking(token, bookingId);
                return ResponseEntity.ok(ApiResponse.success(null, "Xác nhận huỷ vé thành công. CinePoint đã được cộng vào tài khoản của bạn."));
        }

        // POST /api/v1/bookings/check-in [STAFF, ADMIN] — quét QR tại rạp
        @PostMapping("/check-in")
        @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
        public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
                        @AuthenticationPrincipal User currentUser,
                        @RequestParam String qrCode) {
                return ResponseEntity.ok(ApiResponse.success(
                                bookingService.checkIn(currentUser, qrCode), "Check-in thành công"));
        }
}
