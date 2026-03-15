package com.cinema.ticket_booking.config;

import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.GenreRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final PasswordEncoder passwordEncoder;
    // private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("Checking for default test data...");

            // try {
            // jdbcTemplate.execute("ALTER TABLE transactions DROP CONSTRAINT IF EXISTS
            // transactions_type_check");
            // log.info("Dropped transactions_type_check constraint to allow new enum
            // values.");
            // } catch (Exception e) {
            // log.warn("Could not drop transactions_type_check", e);
            // }

            // Create Admin User
            if (!userRepository.existsByEmail("admin@cinema.com")) {
                User admin = User.builder()
                        .email("admin@cinema.com")
                        .password(passwordEncoder.encode("123123123"))
                        .fullName("System Admin")
                        .role(UserRole.ADMIN)
                        .authProvider(AuthProvider.LOCAL)
                        .isActive(true)
                        .phone("0123456789")
                        .build();
                userRepository.save(admin);
                log.info("Created default ADMIN user: admin@cinema.com / admin123");
            }

            // Create Staff User
            if (!userRepository.existsByEmail("staff@cinema.com")) {
                User staff = User.builder()
                        .email("staff@cinema.com")
                        .password(passwordEncoder.encode("123123123"))
                        .fullName("System Staff")
                        .role(UserRole.STAFF)
                        .authProvider(AuthProvider.LOCAL)
                        .isActive(true)
                        .phone("0987654321")
                        .build();
                userRepository.save(staff);
                log.info("Created default STAFF user: staff@cinema.com / staff123");
            }

            // Create Default Genres
            if (genreRepository.count() == 0) {
                List<String> defaultGenres = List.of(
                        "Hành Động", "Viễn Tưởng", "Phiêu Lưu",
                        "Kinh Dị", "Tình Cảm", "Hài Hước",
                        "Tâm Lý", "Hình Sự", "Hoạt Hình",
                        "Tài Liệu", "Thể Thao", "Gia Đình",
                        "Âm Nhạc", "Kỳ Ảo");

                for (String name : defaultGenres) {
                    Genre genre = new Genre();
                    genre.setName(name);
                    genre.setSlug(toSlug(name));
                    genreRepository.save(genre);
                }
                log.info("Created {} default genres", defaultGenres.size());
            }
        };
    }

    private String toSlug(String input) {
        if (input == null || input.isEmpty())
            return "";
        String noWhitespace = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String slug = noWhitespace.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.replaceAll("[^\\w\\s-]", ""); // Remove non-word chars
        slug = slug.trim().replaceAll("\\s+", "-").toLowerCase();
        return slug;
    }
}
