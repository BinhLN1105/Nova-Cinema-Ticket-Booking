package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.impl.StaffDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffDashboardServiceImpl staffDashboardService;

    /**
     * GET /api/v1/staff/dashboard/stats
     * Trả về thống kê nhanh cho Staff Dashboard:
     * - Số suất chiếu hôm nay tại rạp
     * - Vé đã soát hôm nay
     * - Vé đã soát trong tháng
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getDashboardStats(
            @AuthenticationPrincipal User currentUser) {
        StaffDashboardResponse stats = staffDashboardService.getDashboardStats(currentUser);
        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê thành công"));
    }
}
