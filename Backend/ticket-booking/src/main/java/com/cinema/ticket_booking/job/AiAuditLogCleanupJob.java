package com.cinema.ticket_booking.job;

import com.cinema.ticket_booking.repository.AiAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAuditLogCleanupJob {

    private final AiAuditLogRepository auditLogRepository;

    /**
     * Chạy hàng ngày vào lúc 2:00 AM để tự động xóa log AI đã lưu quá 30 ngày.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Starting cleanup of AI audit logs older than {}", cutoff);
        try {
            auditLogRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Successfully cleaned up AI audit logs older than 30 days");
        } catch (Exception e) {
            log.error("Failed to cleanup AI audit logs", e);
        }
    }
}
