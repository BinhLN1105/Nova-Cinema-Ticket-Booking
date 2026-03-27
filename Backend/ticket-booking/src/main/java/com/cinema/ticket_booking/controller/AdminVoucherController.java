package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.service.VoucherService;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;
    private final VoucherMapper voucherMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAll(pageable), "Lấy danh sách voucher thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherMapper.toResponse(voucherService.findById(id)), 
                "Lấy thông tin voucher thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> create(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(voucherService.create(request), "Tạo voucher thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                voucherService.update(id, request), "Cập nhật voucher thành công"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable UUID id) {
        voucherService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật trạng thái voucher"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        voucherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa voucher thành công"));
    }
}
