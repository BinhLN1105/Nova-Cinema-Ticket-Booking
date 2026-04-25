package com.cinema.ticket_booking.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("[Flyway] Kích hoạt Database Migration (Supabase Mode)");

        // Giải phóng advisory lock bị kẹt trước khi Flyway khởi động
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT pg_advisory_unlock_all()");
            log.info("[Flyway] Đã giải phóng advisory lock (nếu có).");
        } catch (Exception e) {
            log.warn("[Flyway] Không thể unlock: {}", e.getMessage());
        }

        // Supabase dùng Transaction Pooler (port 6543) không hỗ trợ pg_advisory_lock.
        // Giải pháp: chỉ validate thay vì migrate để tránh lock hoàn toàn.
        // Migration nên được chạy thủ công qua Flyway CLI hoặc Session Pooler (port 5432).
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .configuration(Map.of(
                    "flyway.postgresql.transactional.lock", "false"
                ))
                .connectRetries(3)
                .load();

        try {
            flyway.migrate();
            log.info("[Flyway] Migration hoàn tất.");
        } catch (Exception e) {
            // Nếu migrate bị lock, thử validate để không crash app
            log.warn("[Flyway] Migration bị lỗi (có thể do advisory lock): {}. Thử validate...", e.getMessage());
            try {
                flyway.validate();
                log.info("[Flyway] Schema đã up-to-date, bỏ qua migration.");
            } catch (Exception ve) {
                log.warn("[Flyway] Validate cũng lỗi: {}. App vẫn tiếp tục.", ve.getMessage());
            }
        }

        return flyway;
    }
}
