package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.dto.response.UpcomingShowtimeItem;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.repository.BookingRepository;
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
                                        String screen = showtime.getScreen() != null ? showtime.getScreen().getName()
                                                        : "N/A";

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
        // Query trực tiếp từ bảng bookings (CHECKED_IN) vì đây là source of truth.
        // filter = "TODAY" hoặc "THIS_MONTH"

        public PageResponse<CheckInHistoryItemResponse> getCheckInHistory(
                        User currentUser, String filter, Pageable pageable) {

                UUID cinemaId = getCinemaId(currentUser);

                // Xác định khoảng thời gian theo filter
                LocalDateTime from;
                LocalDateTime to = LocalDateTime.now().plusSeconds(1);

                if ("TODAY".equalsIgnoreCase(filter)) {
                        from = LocalDate.now().atStartOfDay();
                } else {
                        // THIS_MONTH (mặc định)
                        YearMonth month = YearMonth.now();
                        from = month.atDay(1).atStartOfDay();
                }

                Page<Booking> page = bookingRepository.findCheckedInByCinemaAndDateRange(
                                cinemaId, from, to, pageable);

                List<CheckInHistoryItemResponse> content = page.getContent().stream()
                                .map(booking -> {
                                        // Tổng hợp tên ghế từ booking items
                                        String seatsStr = "";
                                        try {
                                                if (booking.getBookingItems() != null) {
                                                        seatsStr = booking.getBookingItems().stream()
                                                                        .filter(bi -> bi.getShowtimeSeat() != null
                                                                                        && bi.getShowtimeSeat()
                                                                                                        .getSeat() != null)
                                                                        .map(bi -> String.valueOf(bi.getShowtimeSeat()
                                                                                        .getSeat().getRowLabel())
                                                                                        + bi.getShowtimeSeat().getSeat()
                                                                                                        .getColNumber())
                                                                        .sorted()
                                                                        .collect(Collectors.joining(", "));
                                                }
                                        } catch (Exception ignored) {
                                        }

                                        String movieTitle = null;
                                        String posterUrl = null;
                                        String screenName = null;
                                        String cinemaName = null;
                                        LocalDateTime scannedAt = booking.getCreatedAt();

                                        try {
                                                if (booking.getShowtime() != null) {
                                                        if (booking.getShowtime().getMovie() != null) {
                                                                movieTitle = booking.getShowtime().getMovie()
                                                                                .getTitle();
                                                                posterUrl = booking.getShowtime().getMovie()
                                                                                .getPosterUrl();
                                                        }
                                                        if (booking.getShowtime().getScreen() != null) {
                                                                screenName = booking.getShowtime().getScreen()
                                                                                .getName();
                                                        }
                                                }
                                                if (booking.getCinema() != null) {
                                                        cinemaName = booking.getCinema().getName();
                                                }
                                        } catch (Exception ignored) {
                                        }

                                        return CheckInHistoryItemResponse.builder()
                                                        .bookingCode(booking.getBookingCode())
                                                        .customerName(booking.getUser() != null
                                                                        ? booking.getUser().getFullName()
                                                                        : "Khách vãng lai")
                                                        .customerPhone(booking.getUser() != null
                                                                        ? booking.getUser().getPhone()
                                                                        : "")
                                                        .movieTitle(movieTitle != null ? movieTitle : "N/A")
                                                        .moviePosterUrl(posterUrl)
                                                        .screenName(screenName)
                                                        .cinemaName(cinemaName)
                                                        .seatsChecked(seatsStr)
                                                        .success(true) // booking CHECKED_IN = thành công
                                                        .failReason(null)
                                                        .scannedAt(scannedAt)
                                                        .build();
                                })
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
