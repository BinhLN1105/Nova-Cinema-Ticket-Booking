package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.dto.response.UpcomingShowtimeItem;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.impl.StaffDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffDashboardServiceImpl staffDashboardService;

    /**
     * GET /api/v1/staff/dashboard/stats
     * Thống kê nhanh: suất chiếu hôm nay, vé đã soát hôm nay, vé đã soát tháng này.
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getDashboardStats(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                staffDashboardService.getDashboardStats(currentUser), "Lấy thống kê thành công"));
    }

    /**
     * GET /api/v1/staff/dashboard/upcoming-showtimes
     * Danh sách suất chiếu sắp bắt đầu trong 60 phút tới tại rạp của nhân viên.
     */
    @GetMapping("/dashboard/upcoming-showtimes")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UpcomingShowtimeItem>>> getUpcomingShowtimes(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                staffDashboardService.getUpcomingShowtimes(currentUser), "Thành công"));
    }

    /**
     * GET /api/v1/staff/check-in-history?page=0&size=20
     * Lịch sử soát vé (cả thành công và thất bại), phân trang, mới nhất trước.
     */
    @GetMapping("/check-in-history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> getCheckInHistory(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("scannedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                staffDashboardService.getCheckInHistory(currentUser, pageable), "Thành công"));
    }
}
