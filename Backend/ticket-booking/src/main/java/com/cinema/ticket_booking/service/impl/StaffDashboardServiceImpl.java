package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.StaffDashboardResponse;
import com.cinema.ticket_booking.model.StaffProfile;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffDashboardServiceImpl {

    private final StaffProfileRepository staffProfileRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;

    public StaffDashboardResponse getDashboardStats(User currentUser) {
        // Lấy rạp mà nhân viên đang được phân công
        StaffProfile staffProfile = staffProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hồ sơ nhân viên"));

        UUID cinemaId = staffProfile.getCinema().getId();
        LocalDate today = LocalDate.now();

        // Thống kê suất chiếu hôm nay
        long totalShowtimesToday = showtimeRepository.countByCinemaAndDate(cinemaId, today);

        // Thống kê vé đã soát hôm nay
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long ticketsCheckedToday = ticketRepository.countCheckedInByCinemaAndDateRange(
                cinemaId, startOfDay, endOfDay);

        // Thống kê vé đã soát trong tháng
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
}
