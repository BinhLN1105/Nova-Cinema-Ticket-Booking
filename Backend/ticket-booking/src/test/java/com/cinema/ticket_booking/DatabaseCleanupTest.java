package com.cinema.ticket_booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("dev")
public class DatabaseCleanupTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void cleanTestData() {
        System.out.println("=== BẮT ĐẦU DỌN DẸP DATABASE QUA SPRING JDBC ===");

        // 1. Tìm các rạp test
        List<UUID> cinemaIds = jdbcTemplate.queryForList(
            "SELECT id FROM cinemas WHERE name LIKE 'NovaCinema%' OR name LIKE 'E2E Test Cinema%' OR name LIKE 'Nova Cinema%' OR name LIKE 'Cinema Test%'",
            UUID.class
        );
        System.out.println("Tìm thấy " + cinemaIds.size() + " rạp test.");

        // 2. Tìm phim test
        List<UUID> movieIds = jdbcTemplate.queryForList(
            "SELECT id FROM movies WHERE title LIKE 'E2E Test Movie%'",
            UUID.class
        );
        System.out.println("Tìm thấy " + movieIds.size() + " phim test.");

        // 3. Tìm voucher test
        List<UUID> voucherIds = jdbcTemplate.queryForList(
            "SELECT id FROM vouchers WHERE code LIKE 'E2ETEST%' OR code LIKE 'TEST%' OR code LIKE 'BVA%'",
            UUID.class
        );
        System.out.println("Tìm thấy " + voucherIds.size() + " voucher test.");

        // 4. Tìm user test (Staff E2E + BVA Customer)
        List<UUID> userList = jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE email LIKE 'e2e.staff.%' OR email LIKE 'bva%' OR email LIKE 'bval%' OR full_name LIKE 'E2E Staff%'",
            UUID.class
        );
        List<UUID> userIds = new java.util.ArrayList<>(userList);

        // Tìm thêm user_id từ staff_profiles của các rạp test sắp bị xóa
        if (!cinemaIds.isEmpty()) {
            List<UUID> staffUserIdsFromCinemas = jdbcTemplate.queryForList(
                "SELECT user_id FROM staff_profiles WHERE cinema_id IN (" + toSqlList(cinemaIds) + ")",
                UUID.class
            );
            userIds.addAll(staffUserIdsFromCinemas);
        }
        userIds = userIds.stream()
            .distinct()
            .filter(uid -> {
                String email = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, uid);
                return email != null && !email.equalsIgnoreCase("admin_test@novaticket.com") 
                                     && !email.equalsIgnoreCase("staff_test@novaticket.com") 
                                     && !email.equalsIgnoreCase("customer_test@novaticket.com");
            })
            .collect(Collectors.toList());
        System.out.println("Tìm thấy " + userIds.size() + " users test.");

        if (cinemaIds.isEmpty() && movieIds.isEmpty() && voucherIds.isEmpty() && userIds.isEmpty()) {
            System.out.println("Không có dữ liệu test nào cần dọn dẹp.");
            return;
        }

        // 5. Thu thập Screen IDs
        List<UUID> screenIds = List.of();
        if (!cinemaIds.isEmpty()) {
            screenIds = jdbcTemplate.queryForList(
                "SELECT id FROM screens WHERE cinema_id IN (" + toSqlList(cinemaIds) + ")",
                UUID.class
            );
        }

        // 6. Thu thập Showtime IDs
        List<UUID> showtimeIds = new java.util.ArrayList<>();
        if (!screenIds.isEmpty()) {
            showtimeIds.addAll(jdbcTemplate.queryForList(
                "SELECT id FROM showtimes WHERE screen_id IN (" + toSqlList(screenIds) + ")",
                UUID.class
            ));
        }
        if (!movieIds.isEmpty()) {
            showtimeIds.addAll(jdbcTemplate.queryForList(
                "SELECT id FROM showtimes WHERE movie_id IN (" + toSqlList(movieIds) + ")",
                UUID.class
            ));
        }
        showtimeIds = showtimeIds.stream().distinct().collect(Collectors.toList());

        // 7. Thu thập Booking IDs
        List<UUID> bookingIds = new java.util.ArrayList<>();
        if (!cinemaIds.isEmpty()) {
            bookingIds.addAll(jdbcTemplate.queryForList(
                "SELECT id FROM bookings WHERE cinema_id IN (" + toSqlList(cinemaIds) + ")",
                UUID.class
            ));
        }
        if (!showtimeIds.isEmpty()) {
            bookingIds.addAll(jdbcTemplate.queryForList(
                "SELECT id FROM bookings WHERE showtime_id IN (" + toSqlList(showtimeIds) + ")",
                UUID.class
            ));
        }
        if (!userIds.isEmpty()) {
            bookingIds.addAll(jdbcTemplate.queryForList(
                "SELECT id FROM bookings WHERE user_id IN (" + toSqlList(userIds) + ")",
                UUID.class
            ));
        }
        bookingIds = bookingIds.stream().distinct().collect(Collectors.toList());

        // --- TIẾN HÀNH XÓA ---
        System.out.println("Đang xóa dữ liệu liên kết...");

        // Xóa scan_logs trước để tránh lỗi khóa ngoại (foreign key)
        if (tableExists("scan_logs")) {
            if (!cinemaIds.isEmpty()) {
                jdbcTemplate.execute("DELETE FROM scan_logs WHERE cinema_id IN (" + toSqlList(cinemaIds) + ")");
            }
            if (!bookingIds.isEmpty()) {
                jdbcTemplate.execute("DELETE FROM scan_logs WHERE booking_id IN (" + toSqlList(bookingIds) + ")");
            }
            if (!userIds.isEmpty()) {
                jdbcTemplate.execute("DELETE FROM scan_logs WHERE staff_id IN (" + toSqlList(userIds) + ")");
            }
            System.out.println("-> Đã xóa lịch sử soát vé (scan logs).");
        }

        // Xóa transactions, payments, tickets, booking_items, booking_combos, bookings
        if (!bookingIds.isEmpty()) {
            String bookingListStr = toSqlList(bookingIds);
            
            if (tableExists("transactions")) {
                jdbcTemplate.execute("DELETE FROM transactions WHERE reference_id IN (" + bookingListStr + ")");
            }
            jdbcTemplate.execute("DELETE FROM payments WHERE booking_id IN (" + bookingListStr + ")");
            jdbcTemplate.execute("DELETE FROM tickets WHERE booking_id IN (" + bookingListStr + ")");
            jdbcTemplate.execute("DELETE FROM booking_items WHERE booking_id IN (" + bookingListStr + ")");
            jdbcTemplate.execute("DELETE FROM booking_combos WHERE booking_id IN (" + bookingListStr + ")");
            jdbcTemplate.execute("DELETE FROM bookings WHERE id IN (" + bookingListStr + ")");
            System.out.println("-> Đã xóa dữ liệu đặt vé.");
        }

        // Xóa showtime_seats, showtimes
        if (!showtimeIds.isEmpty()) {
            String showtimeListStr = toSqlList(showtimeIds);
            if (tableExists("showtime_seats")) {
                jdbcTemplate.execute("DELETE FROM showtime_seats WHERE showtime_id IN (" + showtimeListStr + ")");
            }
            jdbcTemplate.execute("DELETE FROM showtimes WHERE id IN (" + showtimeListStr + ")");
            System.out.println("-> Đã xóa suất chiếu.");
        }

        // Xóa seats, screens
        if (!screenIds.isEmpty()) {
            String screenListStr = toSqlList(screenIds);
            jdbcTemplate.execute("DELETE FROM seats WHERE screen_id IN (" + screenListStr + ")");
            jdbcTemplate.execute("DELETE FROM screens WHERE id IN (" + screenListStr + ")");
            System.out.println("-> Đã xóa phòng chiếu và ghế.");
        }

        // Xóa users test
        if (!userIds.isEmpty()) {
            String userListStr = toSqlList(userIds);
            jdbcTemplate.execute("DELETE FROM staff_profiles WHERE user_id IN (" + userListStr + ")");
            jdbcTemplate.execute("DELETE FROM user_vouchers WHERE user_id IN (" + userListStr + ")");
            jdbcTemplate.execute("DELETE FROM refresh_tokens WHERE user_id IN (" + userListStr + ")");
            if (tableExists("password_reset_tokens")) {
                jdbcTemplate.execute("DELETE FROM password_reset_tokens WHERE user_id IN (" + userListStr + ")");
            }
            if (tableExists("user_exp_histories")) {
                jdbcTemplate.execute("DELETE FROM user_exp_histories WHERE user_id IN (" + userListStr + ")");
            }
            jdbcTemplate.execute("DELETE FROM users WHERE id IN (" + userListStr + ")");
            System.out.println("-> Đã xóa tài khoản test.");
        }

        // Xóa vouchers test
        if (!voucherIds.isEmpty()) {
            String voucherListStr = toSqlList(voucherIds);
            jdbcTemplate.execute("DELETE FROM user_vouchers WHERE voucher_id IN (" + voucherListStr + ")");
            jdbcTemplate.execute("UPDATE bookings SET voucher_id = null WHERE voucher_id IN (" + voucherListStr + ")");
            jdbcTemplate.execute("DELETE FROM vouchers WHERE id IN (" + voucherListStr + ")");
            System.out.println("-> Đã xóa voucher test.");
        }

        // Xóa movies test
        if (!movieIds.isEmpty()) {
            String movieListStr = toSqlList(movieIds);
            jdbcTemplate.execute("DELETE FROM reviews WHERE movie_id IN (" + movieListStr + ")");
            if (tableExists("movie_embeddings")) {
                jdbcTemplate.execute("DELETE FROM movie_embeddings WHERE movie_id IN (" + movieListStr + ")");
            }
            jdbcTemplate.execute("DELETE FROM movies WHERE id IN (" + movieListStr + ")");
            System.out.println("-> Đã xóa phim test.");
        }

        // Xóa cinemas test
        if (!cinemaIds.isEmpty()) {
            String cinemaListStr = toSqlList(cinemaIds);
            jdbcTemplate.execute("DELETE FROM staff_profiles WHERE cinema_id IN (" + cinemaListStr + ")");
            jdbcTemplate.execute("DELETE FROM cinemas WHERE id IN (" + cinemaListStr + ")");
            System.out.println("-> Đã xóa rạp test.");
        }

        // Reset điểm thưởng (CinePoints) của customer_test để đảm bảo đủ tiền thanh toán E2E Wallet test
        jdbcTemplate.execute("UPDATE users SET reward_points = 2000 WHERE email = 'customer_test@novaticket.com'");

        System.out.println("=== DỌN DẸP DATABASE THÀNH CÔNG ===");
    }

    private String toSqlList(List<UUID> ids) {
        return ids.stream()
            .map(id -> "'" + id.toString() + "'")
            .collect(Collectors.joining(","));
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
