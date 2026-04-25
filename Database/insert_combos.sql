-- Script insert dữ liệu mẫu cho Combo
-- Chạy script này vào database của bạn bằng công cụ như DBeaver, pgAdmin, DataGrip, v.v.

INSERT INTO combos (id, name, description, price, image_url, is_available) VALUES
(gen_random_uuid(), 'Combo Solo', '1 Bắp lớn + 1 Nước ngọt siêu to khổng lồ. Vừa ăn vừa xem phim tẹt ga!', 85000, 'https://cdn.yeep.vn/2023/06/sg-11134201-22100-sivckz96hwiv78.jpg', true),
(gen_random_uuid(), 'Combo Couple', '1 Bắp lớn + 2 Nước ngọt. Phù hợp cho 2 người đi xem phim chung.', 115000, 'https://happyvivu.com/wp-content/uploads/2023/11/gia-bap-nuoc-cgv-1.jpg', true),
(gen_random_uuid(), 'Combo Family', '2 Bắp lớn + 4 Nước ngọt. Lựa chọn tuyệt vời cho gia đình bạn.', 200000, 'https://sg-live-01.slatic.net/p/36409faa468c7725f1604270780d3ece.jpg_525x525q80.jpg', true);

-- Lưu ý: Bạn có thể cần chắc chắn rằng Hibernate config của database có hỗ trợ hàm gen_random_uuid().
-- Nếu dùng MySQL, thay gen_random_uuid() bằng UUID(). Postgresql và SQL Server dùng cú pháp khác nhau.
-- Đây là script DML (Data Manipulation Language).
