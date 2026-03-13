package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RevenueByDay;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.DashboardService;
import com.cinema.ticket_booking.service.AnalyticsService;
import com.cinema.ticket_booking.dto.response.AnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Transactional(readOnly = true)
public class AdminController {

    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    // ── Dashboard ────────────────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(), "Lấy dữ liệu thống kê thành công"));
    }

    @GetMapping("/dashboard/revenue")
    public ResponseEntity<ApiResponse<List<RevenueByDay>>> getRevenue(
            @RequestParam(defaultValue = "month") String period) {
        List<BookingRepository.RevenueByDayProjection> data = bookingRepository.getRevenueByDay();
        List<RevenueByDay> result = new ArrayList<>();

        int days = switch (period) {
            case "week" -> 6;
            case "year" -> 364;
            default -> 29;
        };

        LocalDate today = LocalDate.now();
        for (int i = days; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal dayRev = BigDecimal.ZERO;
            for (BookingRepository.RevenueByDayProjection row : data) {
                if (row.getDate() != null && row.getDate().equals(d.toString())) {
                    dayRev = row.getRevenue();
                    break;
                }
            }
            result.add(new RevenueByDay(d.format(DateTimeFormatter.ISO_DATE), dayRev));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/dashboard/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAnalytics(), "Lấy dữ liệu phân tích thành công"));
    }

    // ── Users (Admin) ────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);

        Page<UserResponse> mapped = users.map(u -> UserResponse.builder()
                .id(u.getId().toString())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .authProvider(u.getAuthProvider())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(mapped)));
    }

    @PatchMapping("/users/{id}/role")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setRole(UserRole.valueOf(body.get("role")));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật vai trò thành công"));
    }

    @PatchMapping("/users/{id}/ban")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công"));
    }

    // ── Bookings (Admin) ─────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse.Summary>>> getBookings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookings;

        if (status != null && !status.isEmpty()) {
            bookings = bookingRepository.findByStatus(BookingStatus.valueOf(status), pageable);
        } else {
            bookings = bookingRepository.findAll(pageable);
        }

        Page<BookingResponse.Summary> mapped = bookings.map(b -> BookingResponse.Summary.builder()
                .id(b.getId().toString())
                .bookingCode(b.getBookingCode())
                .movieTitle(b.getShowtime() != null && b.getShowtime().getMovie() != null
                        ? b.getShowtime().getMovie().getTitle() : "—")
                .moviePosterUrl(b.getShowtime() != null && b.getShowtime().getMovie() != null
                        ? b.getShowtime().getMovie().getPosterUrl() : null)
                .startTime(b.getShowtime() != null ? b.getShowtime().getStartTime() : null)
                .cinemaName(b.getShowtime() != null && b.getShowtime().getScreen() != null
                        && b.getShowtime().getScreen().getCinema() != null
                        ? b.getShowtime().getScreen().getCinema().getName() : "—")
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(mapped)));
    }
}
