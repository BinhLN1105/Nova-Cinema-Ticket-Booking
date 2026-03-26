package com.cinema.ticket_booking.job;

import com.cinema.ticket_booking.enums.ShowtimeStatus;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeClosureJob {

    private final ShowtimeRepository showtimeRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Chạy mỗi 5 phút. Tìm các suất chiếu đang SCHEDULED nhưng đã qua giờ chiếu + LATE_BOOKING_ALLOWANCE_MINS.
     * Chuyển trạng thái sang ONGOING.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    @Transactional
    public void autoCloseShowtimes() {
        log.info("Starting Auto-Close Showtimes Job...");

        int lateAllowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(lateAllowance);

        List<Showtime> expiredShowtimes = showtimeRepository.findExpiredScheduledShowtimes(cutoffTime);
        int count = 0;

        for (Showtime showtime : expiredShowtimes) {
            showtime.setStatus(ShowtimeStatus.ONGOING);
            count++;
        }

        if (count > 0) {
            showtimeRepository.saveAll(expiredShowtimes);
        }

        log.info("Finished Auto-Close Showtimes Job. Closed {} showtimes.", count);
    }
}
