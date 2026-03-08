package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // GET /api/v1/vouchers/validate?code=SUMMER30
    // User kiểm tra voucher trước khi đặt vé (không cần biết minOrder lúc này)
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherResponse.Summary>> validate(
            @RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.getSummaryByCode(code), "Mã hợp lệ"));
    }

    // POST /api/v1/vouchers [ADMIN]
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> create(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(voucherService.create(request), "Tạo voucher thành công"));
    }

    // PUT /api/v1/vouchers/{id} [ADMIN]
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.update(id, request), "Cập nhật voucher thành công"));
    }

    // PATCH /api/v1/vouchers/{id}/toggle [ADMIN] — bật/tắt voucher
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable UUID id) {
        voucherService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật trạng thái voucher"));
    }
}
