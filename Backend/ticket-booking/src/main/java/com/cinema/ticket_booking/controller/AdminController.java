package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.CreateStaffRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse;
import com.cinema.ticket_booking.dto.response.DashboardStatsResponse.RevenueByDay;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.AnalyticsService;
import com.cinema.ticket_booking.service.DashboardService;
import com.cinema.ticket_booking.dto.response.AnalyticsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final StaffProfileRepository staffProfileRepository;
    private final CinemaRepository cinemaRepository;
    private final PasswordEncoder passwordEncoder;

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

    /**
     * N+1-safe: Lấy toàn bộ StaffProfiles của danh sách user bằng MỘT câu JOIN FETCH,
     * thay vì gọi findByUserId() trong vòng lặp.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);

        // Batch query: 1 câu JOIN FETCH duy nhất cho TẤT CẢ user STAFF trong trang này
        List<UUID> userIds = users.map(User::getId).toList();
        Map<UUID, StaffProfile> staffProfileMap = staffProfileRepository
                .findByUserIdInWithCinema(userIds)
                .stream()
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), sp -> sp));

        Page<UserResponse> mapped = users.map(u -> {
            StaffProfile sp = staffProfileMap.get(u.getId());
            return UserResponse.builder()
                    .id(u.getId().toString())
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .phone(u.getPhone())
                    .avatarUrl(u.getAvatarUrl())
                    .role(u.getRole())
                    .authProvider(u.getAuthProvider())
                    .isActive(u.getIsActive())
                    .createdAt(u.getCreatedAt())
                    .cinemaId(sp != null ? sp.getCinema().getId().toString() : null)
                    .cinemaName(sp != null ? sp.getCinema().getName() : null)
                    .build();
        });

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

    /**
     * Ban / Unban user: toggle isActive.
     * Không xóa cứng (hard delete) để giữ lại lịch sử đối soát vé.
     */
    @PatchMapping("/users/{id}/ban")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        String msg = Boolean.TRUE.equals(user.getIsActive())
                ? "Tài khoản đã được kích hoạt" : "Tài khoản đã bị vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success(null, msg));
    }

    // ── Staff Management (Admin only) ────────────────────────────────────

    /**
     * Tạo tài khoản STAFF và gán vào rạp cụ thể — TRONG MỘT TRANSACTION.
     * Nếu tạo StaffProfile thất bại, User sẽ được rollback → không có rác dữ liệu.
     */
    @PostMapping("/staff")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        Cinema cinema = cinemaRepository.findById(UUID.fromString(request.getCinemaId()))
                .orElseThrow(() -> new BadRequestException("Rạp phim không tồn tại"));

        // Tạo User với role STAFF
        User staff = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STAFF)
                .authProvider(AuthProvider.LOCAL)
                .build();
        staff = userRepository.save(staff);

        // Tạo StaffProfile — cùng transaction với User
        StaffProfile profile = StaffProfile.builder()
                .user(staff)
                .cinema(cinema)
                .employeeCode(request.getEmployeeCode())
                .build();
        staffProfileRepository.save(profile);

        UserResponse response = UserResponse.builder()
                .id(staff.getId().toString())
                .email(staff.getEmail())
                .fullName(staff.getFullName())
                .role(staff.getRole())
                .isActive(staff.getIsActive())
                .cinemaId(cinema.getId().toString())
                .cinemaName(cinema.getName())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo tài khoản nhân viên thành công"));
    }

    /**
     * Cập nhật rạp phụ trách của Staff.
     */
    @PatchMapping("/staff/{userId}/cinema")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStaffCinema(
            @PathVariable UUID userId,
            @RequestBody java.util.Map<String, String> body) {

        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Nhân viên không tồn tại"));
        if (staff.getRole() != UserRole.STAFF) {
            throw new BadRequestException("User này không phải là nhân viên");
        }

        Cinema cinema = cinemaRepository.findById(UUID.fromString(body.get("cinemaId")))
                .orElseThrow(() -> new BadRequestException("Rạp phim không tồn tại"));

        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElse(StaffProfile.builder().user(staff).build());
        profile.setCinema(cinema);
        staffProfileRepository.save(profile);

        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật rạp phụ trách thành công"));
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
