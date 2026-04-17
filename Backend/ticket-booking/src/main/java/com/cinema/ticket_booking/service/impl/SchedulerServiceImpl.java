package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.BookingItemRepository;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.repository.RefreshTokenRepository;
import com.cinema.ticket_booking.enums.CampaignStatus;
import com.cinema.ticket_booking.repository.NotificationCampaignRepository;
import com.cinema.ticket_booking.service.NotificationService;
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
    private final NotificationService notificationService;
    private final NotificationCampaignRepository notificationCampaignRepository;

    /**
     * Mỗi 1 phút: giải phóng ghế LOCKED đã hết hạn giữ chỗ.
     * Chạy vào giây thứ 0 của mỗi phút.
     */
    @Override
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredSeatLocks() {
        try {
            int released = showtimeSeatRepository.releaseExpiredLocks(LocalDateTime.now());
            if (released > 0) {
                log.info("[Scheduler] Đã giải phóng {} ghế hết hạn giữ chỗ", released);
            }
        } catch (Exception e) {
            log.warn("[Scheduler] Chưa thể giải phóng ghế: {}", e.getMessage());
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
        try {
            // 1. Lấy danh sách booking sẽ bị expire trước khi UPDATE (không dùng
            // filter(null) nữa)
            List<Booking> toExpire = bookingRepository.findByStatusAndExpiresAtBefore(
                    BookingStatus.PENDING, LocalDateTime.now());

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
        } catch (Exception e) {
            log.warn("[Scheduler] Chưa thể cập nhật booking hết hạn: {}", e.getMessage());
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

    /**
     * Mỗi 5 phút: nhắc nhở khách hàng có suất chiếu sắp bắt đầu (trong 40-45 phút).
     * - Dùng [from, to) → tránh lọt lưới boundary condition.
     * - DB thực hiện lọc thời gian → không load tất cả vé PAID lên RAM.
     * - Bỏ readOnly vì notificationService.sendPromotion() có INSERT vào DB.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendShowtimeReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(40);
        LocalDateTime to = now.plusMinutes(45);

        // DB lọc trực tiếp: startTime >= from AND startTime < to
        List<Booking> upcomingBookings = bookingRepository.findUpcomingBookings(
                BookingStatus.PAID, from, to);

        for (Booking booking : upcomingBookings) {
            String movieTitle = booking.getShowtime().getMovie().getTitle();
            try {
                notificationService.sendPromotion(
                        booking.getUser(),
                        "Sắp đến giờ chiếu! ⏰",
                        "Phim \"" + movieTitle + "\" sẽ bắt đầu sau khoảng 45 phút. Hãy đến rạp chuẩn bị bắp nước nhé!",
                        booking.getId());
            } catch (Exception e) {
                // Catch từng vé để 1 vé lỗi không làm chết cả vòng lặp
                log.error("[Scheduler] Lỗi gửi thông báo cho booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        if (!upcomingBookings.isEmpty()) {
            log.info("[Scheduler] Đã gửi {} nhắc nhở suất chiếu sắp tới", upcomingBookings.size());
        }
    }

    /**
     * Mỗi 1 phút: Xử lý các chiến dịch thông báo đã lên lịch.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledCampaigns() {
        var now = LocalDateTime.now();
        var pendingCampaigns = notificationCampaignRepository.findByStatusAndScheduledAtBefore(
                CampaignStatus.PENDING, now);

        if (pendingCampaigns.isEmpty())
            return;

        log.info("[Scheduler] Đang xử lý {} chiến dịch thông báo...", pendingCampaigns.size());

        for (var campaign : pendingCampaigns) {
            try {
                // 1. Broadcast (Topic + Global Notification)
                notificationService.broadcastGlobalNotification(
                        campaign.getTitle(),
                        campaign.getBody(),
                        campaign.getType(),
                        campaign.getTargetId(),
                        campaign.getTargetTopic());

                // 2. Update Status
                campaign.setStatus(CampaignStatus.SENT);
                notificationCampaignRepository.save(campaign);

                log.info("[Scheduler] ✓ Đã gửi chiến dịch: {}", campaign.getTitle());
            } catch (Exception e) {
                log.error("[Scheduler] ✗ Lỗi khi gửi chiến dịch {}: {}", campaign.getId(), e.getMessage());
            }
        }
    }
}
