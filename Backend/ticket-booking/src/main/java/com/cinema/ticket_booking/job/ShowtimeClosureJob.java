package com.cinema.ticket_booking.job;

import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.ShowtimeStatus;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeClosureJob {

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Chạy mỗi 1 phút. Cập nhật trạng thái suất chiếu dựa trên thời gian thực tế.
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    @Transactional
    public void autoUpdateShowtimeStatuses() {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            int lateAllowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
            LocalDateTime nowMinusAllowance = now.minusMinutes(lateAllowance);

            // 1. Chuyển sang ONGOING (Hết giờ đặt vé nhưng đang trong giờ chiếu)
            List<Showtime> toOngoing = showtimeRepository.findShowtimesToMarkOngoing(nowMinusAllowance, now);
            if (!toOngoing.isEmpty()) {
                toOngoing.forEach(s -> s.setStatus(ShowtimeStatus.ONGOING));
                showtimeRepository.saveAll(toOngoing);
                log.info("Marked {} showtimes as ONGOING", toOngoing.size());
            }

            // 2. Chuyển sang FINISHED (Đã quá giờ kết thúc)
            List<Showtime> toFinished = showtimeRepository.findShowtimesToMarkFinished(now);
            if (!toFinished.isEmpty()) {
                toFinished.forEach(s -> s.setStatus(ShowtimeStatus.FINISHED));
                showtimeRepository.saveAll(toFinished);
                log.info("Marked {} showtimes as FINISHED", toFinished.size());

                // Đánh dấu vé PAID chưa check-in thành EXPIRED (No-show, mất trắng)
                int totalNoShows = 0;
                for (Showtime s : toFinished) {
                    List<Booking> noShows = bookingRepository.findByShowtimeIdAndStatus(s.getId(), BookingStatus.PAID);
                    if (!noShows.isEmpty()) {
                        noShows.forEach(b -> b.setStatus(BookingStatus.EXPIRED));
                        bookingRepository.saveAll(noShows);
                        totalNoShows += noShows.size();
                    }
                }
                if (totalNoShows > 0) {
                    log.info("Marked {} no-show bookings as EXPIRED (No refund given)", totalNoShows);
                }
            }
        } catch (Exception e) {
            log.warn("[ShowtimeJob] Không thể thực hiện tiến trình tự động (có thể Database đang khởi tạo): {}", e.getMessage());
        }
    }
}
