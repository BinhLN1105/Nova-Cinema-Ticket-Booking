package com.cinema.ticket_booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Temporary config to fix NULL version columns for existing records.
 * This can be deleted after the first successful run.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchemaFixConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner fixNullVersions() {
        return args -> {
            log.info("[SchemaFix] Checking for NULL versions in bookings and users tables...");
            
            int bookingsFix = jdbcTemplate.update("UPDATE bookings SET version = 0 WHERE version IS NULL");
            if (bookingsFix > 0) {
                log.info("[SchemaFix] Updated {} bookings with version = 0", bookingsFix);
            }

            // Fix Users version and numeric columns
            jdbcTemplate.execute("UPDATE users SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("UPDATE users SET reward_points = 0 WHERE reward_points IS NULL");
            jdbcTemplate.execute("UPDATE users SET available_exp = 0 WHERE available_exp IS NULL");
            jdbcTemplate.execute("UPDATE users SET membership_tier = 'BRONZE' WHERE membership_tier IS NULL");

            // Fix Bookings version column
            jdbcTemplate.execute("UPDATE bookings SET version = 0 WHERE version IS NULL");
            log.info("[SchemaFix] Schema fix completed.");
        };
    }
}
