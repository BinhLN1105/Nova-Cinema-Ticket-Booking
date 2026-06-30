### Phụ lục B: Báo cáo chi tiết BVA - Đánh giá phim

#### 1. Mô tả bài toán
Hệ thống NovaTicket cho phép người dùng đánh giá (rating) bộ phim sau khi đã xem xong. Yêu cầu đánh giá được xem là hợp lệ khi trường dữ liệu đầu vào thỏa mãn điều kiện:

| Biến đầu vào | Ý nghĩa | Kiểu dữ liệu | Miền giá trị hợp lệ |
| :--- | :--- | :--- | :--- |
| `rating` | Điểm số đánh giá chất lượng phim (số sao) | Số nguyên (Integer) | Từ 1 đến 5 |

#### 2. Xác định lớp tương đương
| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
| :--- | :--- | :--- | :--- | :--- |
| Điểm số Rating | 1 <= rating <= 5 | V3 | rating < 1<br>rating > 5 | X3<br>X4 |

#### 3. Bảng phân tích giá trị biên
| Biến đầu vào | min | min+ | nominal | max- | max | Tag biên |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Điểm số Rating | 1 | 2 | 3 | 4 | 5 | B11(min), B12(min+), B13(nominal), B14(max-), B15(max) |

#### 4. Bảng Test Case chi tiết và Kết quả thực thi
| STT | Tên Test Case | Dữ liệu đầu vào (Input mô phỏng) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái | Tag bao phủ |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | BVA_RAT_01: Biên dưới rating không hợp lệ [Rating = 0] | `rating: 0` | Bị chặn lỗi 400 Bad Request. | ✅ Rating = 0 bị chặn - Trả về 400 Bad Request | PASS | X3 |
| 2 | BVA_RAT_02: Biên dưới rating hợp lệ nhỏ nhất [Rating = 1] | `rating: 1` | API phản hồi hợp lệ. | ✅ Rating = 1 được chấp nhận qua lớp Validate | PASS | V3, B11 |
| 3 | BVA_RAT_03: Biên trên rating hợp lệ lớn nhất [Rating = 5] | `rating: 5` | API phản hồi hợp lệ. | ✅ Rating = 5 được chấp nhận qua lớp Validate | PASS | V3, B15 |
| 4 | BVA_RAT_04: Biên trên rating không hợp lệ [Rating = 6] | `rating: 6` | Bị chặn lỗi 400 Bad Request. | ✅ Rating = 6 bị chặn - Trả về 400 Bad Request | PASS | X4 |