-- Thêm cột rank_usage_this_month vào bảng users
ALTER TABLE users ADD COLUMN rank_usage_this_month integer NOT NULL DEFAULT 0;

-- Thêm cột rank_discount_amount vào bảng bookings
ALTER TABLE bookings ADD COLUMN rank_discount_amount numeric(12,2) NOT NULL DEFAULT 0;
