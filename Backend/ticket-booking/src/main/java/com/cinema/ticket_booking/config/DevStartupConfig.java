package com.cinema.ticket_booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cấu hình dọn dẹp hệ thống chỉ dành cho môi trường Development.
 * Giúp giải phóng ghế và dọn cache mỗi khi restart để thuận tiện cho việc test.
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevStartupConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory connectionFactory;

    @Override
    public void run(String... args) throws Exception {
        log.info("[DevStartup] Đang thực hiện dọn dẹp môi trường Local...");

        // 1. Giải phóng ghế đang bị khóa
        try {
            int unlocked = jdbcTemplate.update("UPDATE showtime_seats SET status = 'AVAILABLE', locked_by = NULL, locked_until = NULL WHERE status = 'LOCKED'");
            if (unlocked > 0) {
                log.info("[DevStartup] Đã giải phóng {} ghế đang bị khóa.", unlocked);
            }
        } catch (Exception e) {
            log.warn("[DevStartup] Lỗi khi giải phóng ghế: {}", e.getMessage());
        }

        // 2. Dọn dẹp Redis
        try {
            connectionFactory.getConnection().serverCommands().flushDb();
            log.info("[DevStartup] Đã dọn dẹp (Flush) Redis DB.");
        } catch (Exception e) {
            log.warn("[DevStartup] Không thể flush Redis: {}", e.getMessage());
        }

        log.info("[DevStartup] Hoàn tất dọn dẹp môi trường Local.");
    }
}
