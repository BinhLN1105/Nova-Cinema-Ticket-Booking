package com.cinema.ticket_booking.scheduler;

import com.cinema.ticket_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyResetScheduler {

    private final UserRepository userRepository;

    // Tự động chạy lúc 00:00 ngày 01 hàng tháng để reset lượt dùng đặc quyền Rank
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMembershipUsage() {
        log.info("🔄 [Hệ Thống] Bắt đầu tự động reset lượt dùng voucher phân hạng cho toàn bộ thành viên...");
        try {
            userRepository.resetAllRankUsage();
            log.info("✨ [Hệ Thống] Đã reset thành công lượt dùng đặc quyền hạng thành viên về 0!");
        } catch (Exception e) {
            log.error("❌ [Hệ Thống] Lỗi xảy ra khi reset lượt dùng đặc quyền hạng thành viên: {}", e.getMessage(), e);
        }
    }
}
