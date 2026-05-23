package com.cinema.ticket_booking.config;

import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("test") // Chỉ chạy khi Spring Profile là "test" (Dùng cho CI/CD và Local Newman Test)
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("[TestDataInit] Đang kiểm tra và khởi tạo dữ liệu tài khoản test cho CI/CD...");

        try {
            // 1. Tạo tài khoản Admin test
            String adminEmail = "admin_test@novaticket.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("AdminPassword123!"))
                        .fullName("Admin Test CI/CD")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                log.info("[TestDataInit] Đã tạo thành công tài khoản ADMIN: {}", adminEmail);
            } else {
                log.info("[TestDataInit] Tài khoản ADMIN đã tồn tại, bỏ qua tạo mới: {}", adminEmail);
            }

            // 2. Tạo tài khoản Staff test
            String staffEmail = "staff_test@novaticket.com";
            if (!userRepository.existsByEmail(staffEmail)) {
                User staff = User.builder()
                        .email(staffEmail)
                        .password(passwordEncoder.encode("StaffPassword123!"))
                        .fullName("Staff Test CI/CD")
                        .role(UserRole.STAFF)
                        .isActive(true)
                        .build();
                userRepository.save(staff);
                log.info("[TestDataInit] Đã tạo thành công tài khoản STAFF: {}", staffEmail);
            } else {
                log.info("[TestDataInit] Tài khoản STAFF đã tồn tại, bỏ qua tạo mới: {}", staffEmail);
            }

            log.info("[TestDataInit] Hoàn tất khởi tạo dữ liệu tài khoản test.");
        } catch (Exception e) {
            log.error("[TestDataInit] Lỗi xảy ra khi khởi tạo tài khoản test: {}", e.getMessage(), e);
        }
    }
}
