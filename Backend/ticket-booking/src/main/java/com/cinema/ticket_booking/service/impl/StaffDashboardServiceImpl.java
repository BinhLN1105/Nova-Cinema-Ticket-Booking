package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.dto.response.UpcomingShowtimeItem;
import com.cinema.ticket_booking.model.ScanLog;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.ScanLogRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffDashboardServiceImpl {

    private final StaffProfileRepository staffProfileRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final ScanLogRepository scanLogRepository;

    // ── Stats Dashboard ─────────────────────────────────────────────────────

    public StaffDashboardResponse getDashboardStats(User currentUser) {
        UUID cinemaId = getCinemaId(currentUser);
        LocalDate today = LocalDate.now();

        long totalShowtimesToday = showtimeRepository.countByCinemaAndDate(cinemaId, today);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long ticketsCheckedToday = ticketRepository.countCheckedInByCinemaAndDateRange(
                cinemaId, startOfDay, endOfDay);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        long ticketsCheckedThisMonth = ticketRepository.countCheckedInByCinemaAndDateRange(
                cinemaId, startOfMonth, endOfMonth);

        return StaffDashboardResponse.builder()
                .totalShowtimesToday(totalShowtimesToday)
                .ticketsCheckedToday(ticketsCheckedToday)
                .ticketsCheckedThisMonth(ticketsCheckedThisMonth)
                .build();
    }

    // ── Upcoming Showtimes ───────────────────────────────────────────────────

    public List<UpcomingShowtimeItem> getUpcomingShowtimes(User currentUser) {
        UUID cinemaId = getCinemaId(currentUser);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusMinutes(60);

        return showtimeRepository.findUpcomingByCinema(cinemaId, now, until)
                .stream()
                .map(showtime -> {
                    long minutes = Duration.between(now, showtime.getStartTime()).toMinutes();
                    String title = "N/A";
                    String poster = null;
                    if (showtime.getMovie() != null) {
                        title = showtime.getMovie().getTitle();
                        poster = showtime.getMovie().getPosterUrl();
                    }
                    String screen = showtime.getScreen() != null ? showtime.getScreen().getName() : "N/A";

                    return UpcomingShowtimeItem.builder()
                            .showtimeId(showtime.getId().toString())
                            .movieTitle(title)
                            .moviePosterUrl(poster)
                            .screenName(screen)
                            .startTime(showtime.getStartTime())
                            .minutesUntilStart(minutes)
                            .urgency(minutes < 15 ? "SOON" : "UPCOMING")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Check-in History ────────────────────────────────────────────────────
    // Sử dụng bảng scan_logs để hiển thị cả Thành công và Thất bại

    public PageResponse<CheckInHistoryItemResponse> getCheckInHistory(User currentUser, Pageable pageable) {
        UUID cinemaId = getCinemaId(currentUser);

        // Nova: Tự động bù đắp dữ liệu log từ bookings nếu phát hiện bảng log đang
        // trống
        // Điều này giúp đồng bộ dữ liệu 1/6 (cũ) của bạn vào lịch sử.
        backfillCheckInLogsIfNeeded(cinemaId);

        Page<ScanLog> page = scanLogRepository.findByCinemaIdOrderByScannedAtDesc(cinemaId, pageable);

        List<CheckInHistoryItemResponse> content = page.getContent().stream()
                .map(log -> CheckInHistoryItemResponse.builder()
                        .bookingCode(log.getBooking() != null ? log.getBooking().getBookingCode() : "N/A")
                        .customerName(log.getCustomerName() != null ? log.getCustomerName() : "Khách vãng lai")
                        .customerPhone(log.getCustomerPhone() != null ? log.getCustomerPhone() : "Chưa có SĐT")
                        .movieTitle(log.getMovieTitle())
                        .moviePosterUrl(log.getMoviePosterUrl())
                        .screenName(log.getScreenName())
                        .seatsChecked(log.getSeatsChecked())
                        .success(log.isSuccess())
                        .failReason(log.getFailReason())
                        .scannedAt(log.getScannedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    /** Nova: Tự động tạo log từ các đơn đã CHECKED_IN cũ để đồng bộ lịch sử */
    private void backfillCheckInLogsIfNeeded(UUID cinemaId) {
        long logCount = scanLogRepository.countByCinemaId(cinemaId);
        if (logCount == 0) {
            // Lấy 20 đơn CHECKED_IN gần nhất để đưa vào log
            List<Booking> recentBookings = bookingRepository
                    .findTop20ByCinemaIdAndStatusOrderByCreatedAtDesc(cinemaId,
                            com.cinema.ticket_booking.enums.BookingStatus.CHECKED_IN);

            for (Booking b : recentBookings) {
                String seatsStr = b.getBookingItems().stream()
                        .filter(bi -> bi.getShowtimeSeat() != null && bi.getShowtimeSeat().getSeat() != null)
                        .map(bi -> bi.getShowtimeSeat().getSeat().getRowLabel()
                                + String.valueOf(bi.getShowtimeSeat().getSeat().getColNumber()))
                        .collect(Collectors.joining(", "));

                ScanLog log = ScanLog.builder()
                        .cinema(b.getCinema())
                        .booking(b)
                        .customerName(b.getUser() != null ? b.getUser().getFullName() : "Khách vãng lai")
                        .customerPhone(b.getUser() != null ? b.getUser().getPhone() : "")
                        .movieTitle(b.getShowtime() != null && b.getShowtime().getMovie() != null
                                ? b.getShowtime().getMovie().getTitle()
                                : "N/A")
                        .moviePosterUrl(b.getShowtime() != null && b.getShowtime().getMovie() != null
                                ? b.getShowtime().getMovie().getPosterUrl()
                                : null)
                        .screenName(b.getShowtime() != null && b.getShowtime().getScreen() != null
                                ? b.getShowtime().getScreen().getName()
                                : "N/A")
                        .seatsChecked(seatsStr)
                        .scannedAt(b.getCreatedAt())
                        .success(true)
                        .build();
                scanLogRepository.save(log);
            }
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private UUID getCinemaId(User currentUser) {
        StaffProfile staffProfile = staffProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hồ sơ nhân viên"));
        return staffProfile.getCinema().getId();
    }
}
