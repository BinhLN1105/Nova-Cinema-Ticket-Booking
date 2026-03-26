package com.cinema.ticket_booking.job;

import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.UserRepository;
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
public class ExpConversionJob {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Chạy mỗi giờ. Tìm các vé PAID, expAdded = false, suất chiếu đã kết thúc.
     * Chuyển đổi pendingExp thành availableExp và nâng hạng.
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào phút 0 của mỗi giờ
    @Transactional
    public void convertPendingExp() {
        log.info("Starting Pending EXP Conversion Job...");

        // Find bookings where showtime's end time is in the past, status is PAID, and
        // EXP is not yet added.
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(3);
        List<Booking> bookings = bookingRepository.findBookingsForExpConversion(BookingStatus.PAID, thresholdTime);

        int penaltyPercent = systemConfigService.getIntConfig("NO_SHOW_EXP_PENALTY_PERCENT", 0);

        int processed = 0;
        for (Booking booking : bookings) {
            if (Boolean.TRUE.equals(booking.getExpAdded()) || booking.getStatus() == BookingStatus.CHECKED_IN) {
                continue;
            }

            User user = booking.getUser();
            Long pendingExp = booking.getPendingExp();

            if (pendingExp != null && pendingExp > 0) {
                long expToAdd = pendingExp;
                if (penaltyPercent > 0 && penaltyPercent <= 100) {
                    expToAdd = pendingExp - (pendingExp * penaltyPercent / 100);
                }

                user.setAvailableExp(user.getAvailableExp() + Math.max(0, expToAdd));
                updateMembershipTier(user);
                userRepository.save(user);
            }

            booking.setExpAdded(true);
            bookingRepository.save(booking);
            processed++;
        }

        log.info("Finished Pending EXP Conversion Job. Processed {} bookings.", processed);
    }

    // Nâng hạng: Bạc (500k), Vàng (3M), Kim Cương (10M) tương đương 500/3000/10000
    // EXP.
    private void updateMembershipTier(User user) {
        long exp = user.getAvailableExp();
        MembershipTier currentTier = user.getMembershipTier();
        MembershipTier newTier = currentTier;

        if (exp >= 10000) {
            newTier = MembershipTier.DIAMOND;
        } else if (exp >= 3000) {
            newTier = MembershipTier.GOLD;
        } else if (exp >= 500) {
            newTier = MembershipTier.SILVER;
        } else {
            newTier = MembershipTier.BRONZE;
        }

        if (newTier != currentTier) {
            log.info("User {} upgraded from {} to {}", user.getEmail(), currentTier, newTier);
            user.setMembershipTier(newTier);
        }
    }
}
