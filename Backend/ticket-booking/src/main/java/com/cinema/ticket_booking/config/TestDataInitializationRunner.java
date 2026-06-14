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
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Profile("test") // Chỉ chạy khi Spring Profile là "test" (Dùng cho CI/CD và Local Newman Test)
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.test.admin-password}")
    private String adminPassword;

    @Value("${app.test.staff-password}")
    private String staffPassword;

    @Value("${app.test.customer-password}")
    private String customerPassword;

    @Override
    public void run(String... args) throws Exception {
        log.info("[TestDataInit] Đang kiểm tra và khởi tạo dữ liệu tài khoản test cho CI/CD...");

        try {
            // 1. Tạo tài khoản Admin test
            String adminEmail = "admin_test@novaticket.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
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
                        .password(passwordEncoder.encode(staffPassword))
                        .fullName("Staff Test CI/CD")
                        .role(UserRole.STAFF)
                        .isActive(true)
                        .build();
                userRepository.save(staff);
                log.info("[TestDataInit] Đã tạo thành công tài khoản STAFF: {}", staffEmail);
            } else {
                log.info("[TestDataInit] Tài khoản STAFF đã tồn tại, bỏ qua tạo mới: {}", staffEmail);
            }

            // 3. Tạo tài khoản Customer test
            String customerEmail = "customer_test@novaticket.com";
            if (!userRepository.existsByEmail(customerEmail)) {
                User customer = User.builder()
                        .email(customerEmail)
                        .password(passwordEncoder.encode(customerPassword))
                        .fullName("Customer Test CI/CD")
                        .role(UserRole.CUSTOMER)
                        .isActive(true)
                        .rewardPoints(2000L) // Ví dụ: 2000 CinePoints = 2.000.000 VNĐ cho E2E Wallet test
                        .build();
                userRepository.save(customer);
                log.info("[TestDataInit] Đã tạo thành công tài khoản CUSTOMER: {} với 2000 CinePoints", customerEmail);
            } else {
                log.info("[TestDataInit] Tài khoản CUSTOMER đã tồn tại, bỏ qua tạo mới: {}", customerEmail);
            }

            log.info("[TestDataInit] Hoàn tất khởi tạo dữ liệu tài khoản test.");
        } catch (Exception e) {
            log.error("[TestDataInit] Lỗi xảy ra khi khởi tạo tài khoản test: {}", e.getMessage(), e);
        }
    }
}
