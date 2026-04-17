package com.cinema.ticket_booking.job;

import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.UserVoucher;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
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
public class BookingCleanupJob {

    private final BookingRepository bookingRepository;
    private final UserVoucherRepository userVoucherRepository;

    /**
     * Chạy mỗi 2 phút. Dọn dẹp các booking PENDING đã quá hạn thanh toán.
     * Đồng thời trả lại Voucher về trạng thái AVAILABLE nếu có.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional
    public void cleanupExpiredPendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Tìm các booking PENDING đã quá hạn expires_at
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);
        
        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING bookings to cleanup", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            // 2. Chuyển trạng thái booking sang EXPIRED
            booking.setStatus(BookingStatus.EXPIRED);
            
            // 3. Nếu booking có dùng voucher, trả UserVoucher về AVAILABLE
            if (booking.getVoucher() != null && booking.getUser() != null) {
                userVoucherRepository.findByUserIdAndVoucherId(booking.getUser().getId(), booking.getVoucher().getId())
                        .ifPresent(uv -> {
                            if (uv.getStatus() == UserVoucherStatus.PENDING) {
                                uv.setStatus(UserVoucherStatus.AVAILABLE);
                                userVoucherRepository.save(uv);
                                log.debug("Rolled back voucher {} to AVAILABLE for user {}", 
                                    booking.getVoucher().getCode(), booking.getUser().getEmail());
                            }
                        });
            }
        }

        bookingRepository.saveAll(expiredBookings);
        log.info("Successfully cleaned up {} expired bookings", expiredBookings.size());
    }
}
