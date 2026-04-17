-- ── REFACTOR SCHEMA FIX ──────────────────────────────────────────────
-- Chuyển đổi từ SchemaFixConfig.java sang Flyway Migration

-- 1. Sửa lỗi dữ liệu NULL cho các cột quan trọng
UPDATE users SET version = 0 WHERE version IS NULL;
UPDATE users SET reward_points = 0 WHERE reward_points IS NULL;
UPDATE users SET available_exp = 0 WHERE available_exp IS NULL;
UPDATE users SET membership_tier = 'BRONZE' WHERE membership_tier IS NULL;
UPDATE bookings SET version = 0 WHERE version IS NULL;
UPDATE screens SET is_deleted = false WHERE is_deleted IS NULL;
UPDATE promotions SET is_active = true WHERE is_active IS NULL;

-- 2. Cập nhật Constraint cho Pricing Rules
ALTER TABLE pricing_rules DROP CONSTRAINT IF EXISTS pricing_rules_rule_type_check;
ALTER TABLE pricing_rules ADD CONSTRAINT pricing_rules_rule_type_check 
    CHECK (rule_type IN ('DAY_OF_WEEK', 'TIME_FRAME', 'DATE_RANGE', 'SEAT_TYPE', 'PROMOTION'));

-- 3. Cập nhật Constraint cho trạng thái Booking
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_status_check 
    CHECK (status IN ('PENDING', 'PAID', 'CHECKED_IN', 'CANCELLED', 'EXPIRED'));

-- 4. Thêm Indices để tối ưu hóa Dashboard (Fix Timeout)
CREATE INDEX IF NOT EXISTS idx_bookings_dashboard_stats ON bookings(status, created_at);
CREATE INDEX IF NOT EXISTS idx_bookings_cinema_id ON bookings(cinema_id);

-- 5. Cho phép showtime_id null cho đơn bắp nước (nếu có)
ALTER TABLE bookings ALTER COLUMN showtime_id DROP NOT NULL;

-- 6. Bảo trì bảng Promotions (Cho phép null image_url khi khởi tạo)
ALTER TABLE promotions ALTER COLUMN image_url DROP NOT NULL;

-- 7. Đảm bảo tính nhất quán cho Pricing Rules
UPDATE pricing_rules SET target_type = 'TICKET' WHERE target_type IS NULL;
UPDATE pricing_rules SET min_ticket_qty = 0 WHERE min_ticket_qty IS NULL;
UPDATE pricing_rules SET min_combo_qty = 0 WHERE min_combo_qty IS NULL;

-- 8. Đảm bảo tính nhất quán cho Combos
UPDATE combos SET type = 'COMBO' WHERE type IS NULL;
