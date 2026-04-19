package com.cinema.ticket_booking.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("[Flyway] Kích hoạt Database Migration (Supabase Mode)");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                // Quan trọng: Tắt transactional lock để tương thích với Supabase Transaction
                // Pooler
                .configuration(Collections.singletonMap("flyway.postgresql.transactional.lock", "false"))
                // .validateOnMigrate(false) // Đã sửa xong - có thể bật lại hoặc xóa nếu muốn
                // .repair()
                .load();

        // flyway.repair(); // Đã chạy xong một lần, bây giờ có thể comment lại
        flyway.migrate();
        return flyway;
    }
}
