-- ── INSERT MASTER DATA ──────────────────────────────────────────────
-- Chỉnh sửa từ DataInitializer.java sang Flyway Migration
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
