package com.cinema.ticket_booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Bind tất cả cấu hình có prefix "app" từ application.yml.
 * Inject bằng @Autowired AppProperties thay vì dùng @Value rải rác.
 *
 * app:
 * jwt:
 * secret: xxx
 * expiry-minutes: 60
 * refresh-expiry-days: 30
 * booking:
 * pending-minutes: 10
 * qr:
 * secret: xxx
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Booking booking = new Booking();
    private final Qr qr = new Qr();

    @Data
    public static class Jwt {
        @NotBlank
        private String secret;

        @Min(1)
        private int expiryMinutes = 60;

        @Min(1)
        private int refreshExpiryDays = 30;
    }

    @Data
    public static class Booking {
        @Min(1)
        private int pendingMinutes = 10;
    }

    @Data
    public static class Qr {
        @NotBlank
        private String secret;
    }
}
