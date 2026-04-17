package com.cinema.ticket_booking.config;

import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Runner quét và khởi tạo các dữ liệu hệ thống cơ bản sau khi ứng dụng đã khởi
 * động thành công.
 * Đảm bảo Flyway đã hoàn tất migration trước khi truy cập database.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializationRunner implements CommandLineRunner {

    private final SystemConfigService systemConfigService;

    @Override
    public void run(String... args) throws Exception {
        log.info("[DataInit] Đang kiểm tra và khởi tạo cấu hình hệ thống...");
        try {
            systemConfigService.initDefaults();
            log.info("[DataInit] Hoàn tất khởi tạo cấu hình hệ thống.");
        } catch (Exception e) {
            log.error("[DataInit] Lỗi khi khởi tạo cấu hình: {}", e.getMessage());
        }
    }
}
