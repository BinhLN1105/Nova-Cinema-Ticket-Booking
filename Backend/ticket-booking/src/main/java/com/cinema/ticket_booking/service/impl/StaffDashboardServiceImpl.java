package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.dto.response.UpcomingShowtimeItem;
import com.cinema.ticket_booking.model.ScanLog;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
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
                    return UpcomingShowtimeItem.builder()
                            .showtimeId(showtime.getId().toString())
                            .movieTitle(showtime.getMovie().getTitle())
                            .moviePosterUrl(showtime.getMovie().getPosterUrl())
                            .screenName(showtime.getScreen().getName())
                            .startTime(showtime.getStartTime())
                            .minutesUntilStart(minutes)
                            .urgency(minutes < 15 ? "SOON" : "UPCOMING")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Check-in History ────────────────────────────────────────────────────

    public PageResponse<CheckInHistoryItemResponse> getCheckInHistory(User currentUser, Pageable pageable) {
        UUID cinemaId = getCinemaId(currentUser);
        Page<ScanLog> page = scanLogRepository.findByCinemaIdOrderByScannedAtDesc(cinemaId, pageable);

        List<CheckInHistoryItemResponse> content = page.getContent().stream()
                .map(log -> CheckInHistoryItemResponse.builder()
                        .bookingCode(log.getBooking() != null ? log.getBooking().getBookingCode() : null)
                        .customerName(log.getCustomerName())
                        .customerPhone(log.getCustomerPhone())
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

    // ── Helper ──────────────────────────────────────────────────────────────

    private UUID getCinemaId(User currentUser) {
        StaffProfile staffProfile = staffProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hồ sơ nhân viên"));
        return staffProfile.getCinema().getId();
    }
}
