-- Script insert dữ liệu mẫu cho Combo
-- Chuyển từ Database/insert_combos.sql sang Flyway

INSERT INTO combos (id, name, description, price, image_url, is_available, type) VALUES
(gen_random_uuid(), 'Combo Solo', '1 Bắp lớn + 1 Nước ngọt siêu to khổng lồ. Vừa ăn vừa xem phim tẹt ga!', 85000, 'https://cdn.yeep.vn/2023/06/sg-11134201-22100-sivckz96hwiv78.jpg', true, 'COMBO'),
(gen_random_uuid(), 'Combo Couple', '1 Bắp lớn + 2 Nước ngọt. Phù hợp cho 2 người đi xem phim chung.', 115000, 'https://happyvivu.com/wp-content/uploads/2023/11/gia-bap-nuoc-cgv-1.jpg', true, 'COMBO'),
(gen_random_uuid(), 'Combo Family', '2 Bắp lớn + 4 Nước ngọt. Lựa chọn tuyệt vời cho gia đình bạn.', 200000, 'https://sg-live-01.slatic.net/p/36409faa468c7725f1604270780d3ece.jpg_525x525q80.jpg', true, 'COMBO');
