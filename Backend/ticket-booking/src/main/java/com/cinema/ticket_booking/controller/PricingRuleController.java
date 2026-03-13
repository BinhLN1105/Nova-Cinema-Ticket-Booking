package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PricingRuleRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PricingRuleResponse;
import com.cinema.ticket_booking.service.PricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pricing-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @GetMapping
    public ApiResponse<PageResponse<PricingRuleResponse>> getAll(Pageable pageable) {
        return ApiResponse.success(pricingRuleService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PricingRuleResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(pricingRuleService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PricingRuleResponse> create(@Valid @RequestBody PricingRuleRequest request) {
        return ApiResponse.success(pricingRuleService.create(request), "Tạo quy tắc giá thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<PricingRuleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PricingRuleRequest request) {
        return ApiResponse.success(pricingRuleService.update(id, request), "Cập nhật quy tắc giá thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable UUID id) {
        pricingRuleService.delete(id);
        return ApiResponse.success(null, "Xóa quy tắc giá thành công");
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<PricingRuleResponse> toggleActive(@PathVariable UUID id) {
        return ApiResponse.success(pricingRuleService.toggleActive(id), "Chuyển đổi trạng thái quy tắc thành công");
    }
}
