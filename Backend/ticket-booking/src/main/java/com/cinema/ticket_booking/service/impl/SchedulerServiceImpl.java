package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.BookingItemRepository;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Mỗi 1 phút: giải phóng ghế LOCKED đã hết hạn giữ chỗ.
     * Chạy vào giây thứ 0 của mỗi phút.
     */
    @Override
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredSeatLocks() {
        int released = showtimeSeatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) {
            log.info("[Scheduler] Đã giải phóng {} ghế hết hạn giữ chỗ", released);
        }
    }

    /**
     * Mỗi 2 phút: chuyển booking PENDING hết hạn → EXPIRED,
     * đồng thời giải phóng ghế tương ứng.
     */
    @Override
    @Scheduled(cron = "0 */2 * * * *")
    @Transactional
    public void expireOverdueBookings() {
        // 1. Lấy danh sách booking sẽ bị expire trước khi UPDATE
        List<Booking> toExpire = bookingRepository
                .findByUserIdAndStatus(null, BookingStatus.PENDING)
                .stream()
                .filter(b -> b.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();

        // 2. Giải phóng ghế của từng booking
        for (Booking booking : toExpire) {
            bookingItemRepository.findByBookingIdWithSeat(booking.getId()).forEach(item -> {
                var ss = item.getShowtimeSeat();
                if (ss.getStatus() == SeatStatus.LOCKED) {
                    ss.setStatus(SeatStatus.AVAILABLE);
                    ss.setLockedBy(null);
                    ss.setLockedUntil(null);
                    showtimeSeatRepository.save(ss);
                }
            });
        }

        // 3. Bulk update status → EXPIRED
        int expired = bookingRepository.expireOverdueBookings(LocalDateTime.now());
        if (expired > 0) {
            log.info("[Scheduler] Đã expire {} booking quá hạn thanh toán", expired);
        }
    }

    /**
     * Mỗi ngày lúc 03:00: dọn dẹp refresh token hết hạn trong DB.
     */
    @Override
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredRefreshTokens() {
        refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
        log.info("[Scheduler] Đã dọn dẹp refresh token hết hạn");
    }
}
