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
import com.cinema.ticket_booking.enums.PaymentMethod;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) UUID cinemaId) {

        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().with(LocalTime.MAX);
        }
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(startDate, endDate, cinemaId),
                "Lấy dữ liệu phân tích doanh thu thành công"));
    }

    @GetMapping("/dashboard/revenue")
    public ResponseEntity<ApiResponse<List<RevenueByDay>>> getRevenue(
            @RequestParam(defaultValue = "month") String period) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        boolean isYearly = "year".equals(period);

        int units = switch (period) {
            case "week" -> 7;
            case "year" -> 12;
            default -> 30; // month
        };

        if (isYearly) {
            startDate = now.minusMonths(units - 1).withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            startDate = now.minusDays(units - 1).with(LocalTime.MIN);
        }

        LocalDateTime endDate = now.with(LocalTime.MAX);
        List<BookingRepository.RevenueByDayProjection> data = bookingRepository.getRevenueByDayInRange(startDate,
                endDate, null);

        List<RevenueByDay> result = new ArrayList<>();
        DateTimeFormatter formatter = isYearly ? DateTimeFormatter.ofPattern("yyyy-MM") : DateTimeFormatter.ISO_DATE;

        for (int i = 0; i < units; i++) {
            String targetDateStr = isYearly
                    ? now.minusMonths(units - 1 - i).format(formatter)
                    : now.minusDays(units - 1 - i).format(formatter);

            BigDecimal rev = BigDecimal.ZERO;
            Long count = 0L;

            for (BookingRepository.RevenueByDayProjection row : data) {
                String rowDate = row.getDate() != null ? row.getDate().toString() : "";
                if (rowDate.equals(targetDateStr)) {
                    rev = row.getRevenue();
                    count = row.getBookingCount() != null ? row.getBookingCount() : 0L;
                    break;
                }
            }
            result.add(new RevenueByDay(targetDateStr, rev, BigDecimal.ZERO, BigDecimal.ZERO, count));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/dashboard/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) UUID cinemaId) {

        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().with(LocalTime.MAX);
        }

        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAnalytics(startDate, endDate, cinemaId),
                "Lấy dữ liệu phân tích thành công"));
    }

    // ── Users (Admin) ────────────────────────────────────────────────────

    /**
     * N+1-safe: Lấy toàn bộ StaffProfiles của danh sách user bằng MỘT câu JOIN
     * FETCH,
     * thay vì gọi findByUserId() trong vòng lặp.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        UserRole roleEnum = null;
        if (role != null && !role.isEmpty()) {
            try {
                roleEnum = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid role
            }
        }

        Page<User> users = userRepository.searchUsers(roleEnum, isActive, search, pageable);

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
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // CHỐT CHẶN BẢO MẬT: Không cho phép Admin tự đổi Role của chính mình
        if (id.equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không thể tự thay đổi vai trò của chính mình.");
        }

        // CHỐT CHẶN BẢO MẬT: Không cho phép Admin đổi Role của Admin khác
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Hệ thống bảo mật chặn thao tác trên tài khoản Quản trị khác.");
        }

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
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // CHỐT CHẶN BẢO MẬT: Không cho phép Admin tự khoá chính mình
        if (id.equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không thể tự khoá tài khoản của chính mình.");
        }

        // CHỐT CHẶN BẢO MẬT: Không cho phép Admin khoá Admin khác
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Không thể khoá tài khoản Quản trị viên khác.");
        }

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        String msg = Boolean.TRUE.equals(user.getIsActive())
                ? "Tài khoản đã được kích hoạt"
                : "Tài khoản đã bị vô hiệu hóa";
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
            @RequestBody Map<String, String> body) {

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
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal User currentUser) {

        // 1. Phân quyền Cinema
        UUID activeCinemaId = cinemaId;
        if (currentUser.getRole() == UserRole.STAFF) {
            activeCinemaId = staffProfileRepository.findByUserId(currentUser.getId())
                    .map(sp -> sp.getCinema().getId())
                    .orElseThrow(() -> new BadRequestException("Nhân viên chưa được gán rạp"));
        }

        // 2. Mặc định thời gian (Nếu không truyền)
        if (startDate == null) {
            startDate = LocalDateTime.now().with(LocalTime.MIN);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().with(LocalTime.MAX);
        }

        // 3. Parse Enums
        BookingStatus statusEnum = (status != null && !status.isEmpty()) ? BookingStatus.valueOf(status) : null;
        PaymentMethod pmEnum = (paymentMethod != null && !paymentMethod.isEmpty())
                ? PaymentMethod.valueOf(paymentMethod)
                : null;

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Booking> pageData = bookingRepository.searchBookings(
                startDate, endDate, activeCinemaId, statusEnum, pmEnum, search, pageable);

        Page<BookingResponse.Summary> mapped = pageData.map(b -> {
            boolean isConcessionOnly = b.getShowtime() == null;
            String cinemaName = "—";
            if (b.getCinema() != null) {
                cinemaName = b.getCinema().getName();
            } else if (!isConcessionOnly && b.getShowtime() != null) {
                cinemaName = b.getShowtime().getScreen().getCinema().getName();
            }

            return BookingResponse.Summary.builder()
                    .id(b.getId().toString())
                    .bookingCode(b.getBookingCode())
                    .movieTitle(isConcessionOnly ? "🍿 Hóa đơn F&B" : b.getShowtime().getMovie().getTitle())
                    .moviePosterUrl(!isConcessionOnly ? b.getShowtime().getMovie().getPosterUrl() : null)
                    .startTime(!isConcessionOnly ? b.getShowtime().getStartTime() : null)
                    .cinemaName(cinemaName)
                    .screenName(!isConcessionOnly ? b.getShowtime().getScreen().getName() : "—")
                    .totalAmount(b.getTotalAmount())
                    .status(b.getStatus())
                    .paymentMethod(b.getPaymentMethod())
                    .createdAt(b.getCreatedAt())
                    .build();
        });

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(mapped)));
    }
}
