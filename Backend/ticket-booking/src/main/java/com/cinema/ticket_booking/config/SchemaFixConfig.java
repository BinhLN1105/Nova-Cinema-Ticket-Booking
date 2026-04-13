package com.cinema.ticket_booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cấu hình bảo trì Database và Cache khi khởi động.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchemaFixConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RedisCacheManager cacheManager;
    private final RedisConnectionFactory connectionFactory;

    @Override
    public void run(String... args) throws Exception {
        log.info("[SchemaFix] Bắt đầu kiểm tra và bảo trì hệ thống...");

        // 1. ƯU TIÊN HÀNG ĐẦU: Dọn dẹp Cache và giải phóng ghế để có thể test ngay
        flushRedis();
        unlockAllSeats();
        
        clearCache("movies_now_showing");
        clearCache("movies_coming_soon");
        clearCache("promotions");
        clearCache("combos");
        clearCache("system_configs");

        // 2. Sửa lỗi dữ liệu NULL cho các cột quan trọng
        try {
            jdbcTemplate.execute("UPDATE users SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("UPDATE users SET reward_points = 0 WHERE reward_points IS NULL");
            jdbcTemplate.execute("UPDATE users SET available_exp = 0 WHERE available_exp IS NULL");
            jdbcTemplate.execute("UPDATE users SET membership_tier = 'BRONZE' WHERE membership_tier IS NULL");
            jdbcTemplate.execute("UPDATE bookings SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("UPDATE screens SET is_deleted = false WHERE is_deleted IS NULL");
            jdbcTemplate.execute("UPDATE promotions SET is_active = true WHERE is_active IS NULL");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi cập nhật dữ liệu NULL: {}", e.getMessage());
        }

        // 3. Bảo trì bảng Seats (Grid columns)
        ensureGridColumns();

        // 4. Cập nhật Constraint cho Pricing Rules
        updatePricingRulesConstraint();

        // 5. Thêm các cột Bundle cho Pricing Rules (Dynamic Pricing Engine)
        ensurePricingRulesBundleColumns();
        
        // 6. Thêm các cột Khuyến mãi cho Đơn hàng (Persistence Promotion)
        ensureBookingPromotionColumns();

        // 6.1 Thêm Indices để tối ưu hóa Dashboard (Fix Timeout)
        ensureBookingIndices();

        // 7. Cập nhật Constraint cho trạng thái Booking (Tránh lỗi CHECKED_IN)
        updateBookingStatusConstraint();

        // 8. Đảm bảo cột earned_exp (đã rename từ pending_exp)
        ensureBookingExpColumns();

        // 9. Đảm bảo cấu trúc bảng combos (Loại hình Combo/Bán lẻ)
        ensureComboColumns();

        // 10. Đảm bảo bảng promotions cho phép null imageUrl (Chicken-and-egg fix)
        ensurePromotionColumns();

        // 11. Đảm bảo bảng user_exp_history tồn tại
        ensureUserExpHistoryTable();

        // 12. Cho phép showtime_id null cho đơn bắp nước
        ensureBookingShowtimeNullable();

        // 13. Đảm bảo các cột tùy chỉnh thông báo cho User (Fix 500 Column Not Found)
        ensureUserNotificationColumns();

        // 14. Đảm bảo bảng notification_campaigns tồn tại (Fix SQLGrammarException)
        ensureNotificationCampaignsTable();

        log.info("[SchemaFix] Hoàn tất kiểm tra và bảo trì hệ thống.");
    }

    private void ensureNotificationCampaignsTable() {
        try {
            log.info("[SchemaFix] Đảm bảo bảng 'notification_campaigns' tồn tại...");
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS notification_campaigns (" +
                "  id UUID PRIMARY KEY," +
                "  title VARCHAR(150) NOT NULL," +
                "  body TEXT NOT NULL," +
                "  type VARCHAR(30) NOT NULL," +
                "  target_id UUID," +
                "  target_topic VARCHAR(50) NOT NULL," +
                "  scheduled_at TIMESTAMP NOT NULL," +
                "  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                "  created_by_id UUID REFERENCES users(id)," +
                "  created_at TIMESTAMP," +
                "  updated_at TIMESTAMP" +
                ")"
            );
            log.info("[SchemaFix] Bảng 'notification_campaigns' đã sẵn sàng.");
        } catch (Exception e) {
            log.error("[SchemaFix] Lỗi khi tạo bảng notification_campaigns: {}", e.getMessage());
        }
    }

    private void ensureUserNotificationColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra cấu trúc bảng 'users' (Notification Settings)...");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS allow_marketing_notification BOOLEAN DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS allow_transaction_notification BOOLEAN DEFAULT TRUE");
            
            // Cập nhật giá trị TRUE cho dữ liệu cũ nếu bị NULL (mặc dù JPA đã có @Builder.Default)
            jdbcTemplate.execute("UPDATE users SET allow_marketing_notification = TRUE WHERE allow_marketing_notification IS NULL");
            jdbcTemplate.execute("UPDATE users SET allow_transaction_notification = TRUE WHERE allow_transaction_notification IS NULL");
            
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN allow_marketing_notification SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN allow_transaction_notification SET NOT NULL");
            
            log.info("[SchemaFix] Đã đảm bảo các cột tùy chỉnh thông báo trong bảng 'users'.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi bảo trì bảng users (Notification columns): {}", e.getMessage());
        }
    }

    private void clearCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("[SchemaFix] Đã xóa cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("[SchemaFix] Không thể xóa cache {}: {}", cacheName, e.getMessage());
        }
    }

    private void ensureGridColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra cấu trúc bảng 'seats'...");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS grid_row INT");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS grid_col INT");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS seat_label VARCHAR(10)");

            int migrated = jdbcTemplate.update(
                    "UPDATE seats SET " +
                    "  grid_row = ASCII(row_label) - 65, " +
                    "  grid_col = col_number - 1, " +
                    "  seat_label = row_label || CAST(col_number AS TEXT) " +
                    "WHERE grid_row IS NULL OR grid_col IS NULL OR (grid_row = 0 AND row_label != 'A') OR (grid_col = 0 AND col_number != 1)");

            if (migrated > 0) log.info("[SchemaFix] Đã chuyển đổi {} ghế sang dạng grid", migrated);

            jdbcTemplate.execute("ALTER TABLE seats ALTER COLUMN grid_row SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE seats ALTER COLUMN grid_col SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE seats ALTER COLUMN seat_label SET NOT NULL");
        } catch (Exception e) {
            log.error("[SchemaFix] Lỗi khi bảo trì bảng seats: {}", e.getMessage());
        }
    }

    private void updatePricingRulesConstraint() {
        try {
            log.info("[SchemaFix] Cập nhật ràng buộc cho bảng 'pricing_rules'...");
            jdbcTemplate.execute("ALTER TABLE pricing_rules DROP CONSTRAINT IF EXISTS pricing_rules_rule_type_check");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ADD CONSTRAINT pricing_rules_rule_type_check " +
                    "CHECK (rule_type IN ('DAY_OF_WEEK', 'TIME_FRAME', 'DATE_RANGE', 'SEAT_TYPE', 'PROMOTION'))");
        } catch (Exception e) {
            log.warn("[SchemaFix] Không thể cập nhật constraint pricing_rules: {}", e.getMessage());
        }
    }

    private void ensurePricingRulesBundleColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra các cột Bundle cho 'pricing_rules'...");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ADD COLUMN IF NOT EXISTS target_type VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ADD COLUMN IF NOT EXISTS min_ticket_qty INTEGER");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ADD COLUMN IF NOT EXISTS min_combo_qty INTEGER");

            jdbcTemplate.execute("UPDATE pricing_rules SET target_type = 'TICKET' WHERE target_type IS NULL");
            jdbcTemplate.execute("UPDATE pricing_rules SET min_ticket_qty = 0 WHERE min_ticket_qty IS NULL");
            jdbcTemplate.execute("UPDATE pricing_rules SET min_combo_qty = 0 WHERE min_combo_qty IS NULL");

            jdbcTemplate.execute("ALTER TABLE pricing_rules ALTER COLUMN target_type SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ALTER COLUMN min_ticket_qty SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE pricing_rules ALTER COLUMN min_combo_qty SET NOT NULL");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi cập nhật cột Bundle: {}", e.getMessage());
        }
    }

    private void ensureBookingPromotionColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra các cột Khuyến mãi cho 'bookings'...");
            
            // Giảm timeout để không làm treo cả app nếu DB bận
            try {
                jdbcTemplate.execute("SET statement_timeout = 5000"); // 5 giây cho mỗi lệnh DDL
            } catch (Exception e) {
                log.debug("Không thể set statement_timeout: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS promotion_discount_amount NUMERIC(12,2) DEFAULT 0");
                log.info("[SchemaFix] Đã đảm bảo cột 'promotion_discount_amount' tồn tại.");
            } catch (Exception e) {
                log.warn("[SchemaFix] Bỏ qua lỗi thêm cột promotion_discount_amount (có thể do timeout): {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS applied_promotion_name VARCHAR(255)");
                log.info("[SchemaFix] Đã đảm bảo cột 'applied_promotion_name' tồn tại.");
            } catch (Exception e) {
                log.warn("[SchemaFix] Bỏ qua lỗi thêm cột applied_promotion_name: {}", e.getMessage());
            }
            
            // Reset timeout về mặc định
            try {
                jdbcTemplate.execute("SET statement_timeout = 0");
            } catch (Exception ignore) {}

        } catch (Exception e) {
            log.error("[SchemaFix] Lỗi tổng quát khi bảo trì bookings: {}", e.getMessage());
        }
    }

    private void ensureBookingIndices() {
        try {
            log.info("[SchemaFix] Đang tạo indices cho bảng 'bookings' để tăng tốc Dashboard...");
            // Index cho thống kê doanh thu theo thời gian và trạng thái
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_bookings_dashboard_stats ON bookings(status, created_at)");
            // Index cho việc tìm kiếm theo cinema
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_bookings_cinema_id ON bookings(cinema_id)");
            log.info("[SchemaFix] Đã hoàn tất tạo indices cho 'bookings'.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Không thể tạo indices: {}. Có thể DB đang bận.", e.getMessage());
        }
    }

    private void updateBookingStatusConstraint() {
        try {
            log.info("[SchemaFix] Cập nhật ràng buộc trạng thái cho bảng 'bookings'...");
            jdbcTemplate.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check");
            jdbcTemplate.execute("ALTER TABLE bookings ADD CONSTRAINT bookings_status_check " +
                    "CHECK (status IN ('PENDING', 'PAID', 'CHECKED_IN', 'CANCELLED', 'EXPIRED'))");
            log.info("[SchemaFix] Đã cập nhật thành công danh sách trạng thái hợp lệ cho Booking.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Không thể cập nhật constraint bookings_status: {}", e.getMessage());
        }
    }
    
    private void unlockAllSeats() {
        try {
            log.info("[SchemaFix] Đang giải phóng toàn bộ ghế đang bị khóa (LOCKED) trên hệ thống...");
            // Chuyển toàn bộ ghế từ LOCKED sang AVAILABLE
            int unlocked = jdbcTemplate.update("UPDATE showtime_seats SET status = 'AVAILABLE', locked_by = NULL, locked_until = NULL WHERE status = 'LOCKED'");
            if (unlocked > 0) {
                log.info("[SchemaFix] Đã giải phóng thành công {} ghế đang bị khóa.", unlocked);
            } else {
                log.info("[SchemaFix] Không có ghế nào đang bị khóa cần giải phóng.");
            }
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi giải phóng ghế: {}", e.getMessage());
        }
    }

    private void ensureComboColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra cấu trúc bảng 'combos'...");
            jdbcTemplate.execute("ALTER TABLE combos ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT 'COMBO'");
            jdbcTemplate.execute("UPDATE combos SET type = 'COMBO' WHERE type IS NULL");
            jdbcTemplate.execute("ALTER TABLE combos ALTER COLUMN type SET NOT NULL");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi bảo trì bảng combos: {}", e.getMessage());
        }
    }

    private void ensureBookingExpColumns() {
        try {
            log.info("[SchemaFix] Đảm bảo cột 'earned_exp' trong 'bookings'...");
            
            // Thử rename nếu còn cột cũ
            try {
                jdbcTemplate.execute("ALTER TABLE bookings RENAME COLUMN pending_exp TO earned_exp");
                log.info("[SchemaFix] Đã đổi tên cột 'pending_exp' -> 'earned_exp'.");
            } catch (Exception ignore) {}

            jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS earned_exp BIGINT DEFAULT 0");
            jdbcTemplate.execute("UPDATE bookings SET earned_exp = 0 WHERE earned_exp IS NULL");
        } catch (Exception e) {
            log.error("[SchemaFix] Lỗi khi bảo trì cột earned_exp: {}", e.getMessage());
        }
    }

    private void ensurePromotionColumns() {
        try {
            log.info("[SchemaFix] Đang kiểm tra cấu trúc bảng 'promotions'...");
            // Gỡ bỏ NOT NULL cho image_url để cho phép tạo record trước khi upload ảnh
            jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN image_url DROP NOT NULL");
            log.info("[SchemaFix] Đã đảm bảo cột 'image_url' cho phép giá trị NULL.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi bảo trì bảng promotions: {}", e.getMessage());
        }
    }

    private void flushRedis() {
        try {
            log.info("[SchemaFix] Đang thực hiện FLUSH Redis để tránh lỗi Serialization...");
            connectionFactory.getConnection().serverCommands().flushDb();
            log.info("[SchemaFix] Đã dọn dẹp xong toàn bộ Redis DB.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Không thể flush Redis: {}", e.getMessage());
        }
    }

    private void ensureUserExpHistoryTable() {
        try {
            log.info("[SchemaFix] Đảm bảo bảng 'user_exp_history' tồn tại...");
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS user_exp_history (" +
                "  id UUID PRIMARY KEY," +
                "  user_id UUID NOT NULL REFERENCES users(id)," +
                "  amount BIGINT NOT NULL," +
                "  reason VARCHAR(100)," +
                "  reference_id VARCHAR(100)," +
                "  created_at TIMESTAMP" +
                ")"
            );
            log.info("[SchemaFix] Bảng 'user_exp_history' đã sẵn sàng.");
        } catch (Exception e) {
            log.error("[SchemaFix] Lỗi khi tạo bảng user_exp_history: {}", e.getMessage());
        }
    }

    private void ensureBookingShowtimeNullable() {
        try {
            log.info("[SchemaFix] Đảm bảo cột 'showtime_id' trong 'bookings' cho phép NULL...");
            jdbcTemplate.execute("ALTER TABLE bookings ALTER COLUMN showtime_id DROP NOT NULL");
            log.info("[SchemaFix] Cột 'showtime_id' đã được chuyển sang chế độ Nullable.");
        } catch (Exception e) {
            log.warn("[SchemaFix] Lỗi khi chuyển showtime_id sang Nullable (có thể đã là Nullable): {}", e.getMessage());
        }
    }
}
