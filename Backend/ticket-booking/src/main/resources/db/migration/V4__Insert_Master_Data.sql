-- ── INSERT MASTER DATA ──────────────────────────────────────────────
-- Chỉnh sửa từ DataInitializer.java sang Flyway Migration

-- 1. Chèn hoặc Cập nhật người dùng ADMIN (Mật khẩu: 123123123)
INSERT INTO users (id, email, password, full_name, role, auth_provider, is_active, phone, reward_points, available_exp, membership_tier, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), 
    'admin@cinema.com', 
    '$2a$10$7R8P.T9O.p1eM/R3y6uMbuF1pPZJ2fXq8GqVz5S9Z9vC.H6uC.N6W', 
    'System Admin', 
    'ADMIN', 
    'LOCAL', 
    true, 
    '0123456789', 
    0, 0, 'BRONZE', 0, NOW(), NOW()
)
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password,
    role = EXCLUDED.role;

-- 2. Chèn hoặc Cập nhật người dùng STAFF (Mật khẩu: 123123123)
INSERT INTO users (id, email, password, full_name, role, auth_provider, is_active, phone, reward_points, available_exp, membership_tier, version, created_at, updated_at)
VALUES (
    gen_random_uuid(), 
    'staff@cinema.com', 
    '$2a$10$7R8P.T9O.p1eM/R3y6uMbuF1pPZJ2fXq8GqVz5S9Z9vC.H6uC.N6W', 
    'System Staff', 
    'STAFF', 
    'LOCAL', 
    true, 
    '0987654321', 
    0, 0, 'BRONZE', 0, NOW(), NOW()
)
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password,
    role = EXCLUDED.role;

-- 3. Chèn danh sách Thể loại (Genres) nếu chưa tồn tại
INSERT INTO genres (name, slug)
VALUES 
    ('Hành Động', 'hanh-dong'),
    ('Viễn Tưởng', 'vien-tuong'),
    ('Phiêu Lưu', 'phieu-luu'),
    ('Kinh Dị', 'kinh-di'),
    ('Tình Cảm', 'tinh-cam'),
    ('Hài Hước', 'hai-huoc'),
    ('Tâm Lý', 'tam-ly'),
    ('Hình Sự', 'hinh-su'),
    ('Hoạt Hình', 'hoat-hinh'),
    ('Tài Liệu', 'tai-lieu'),
    ('Thể Thao', 'the-thao'),
    ('Gia Đình', 'gia-dinh'),
    ('Âm Nhạc', 'am-nhac'),
    ('Kỳ Ảo', 'ky-ao')
ON CONFLICT (name) DO NOTHING;
