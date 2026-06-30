# PHỤ LỤC C: BÁO CÁO PHÂN TÍCH GIÁ TRỊ BIÊN (BVA) - GIỚI HẠN ĐẶT VÉ
**Module:** Booking (Giới hạn số lượng Ghế và Combo)
**Người thực hiện:** Nguyễn Minh Thắng
**Mục tiêu:** Kiểm thử các ràng buộc giới hạn đối với số lượng ghế tối đa trên một đơn hàng (1-6 ghế) và tổng số lượng combo (0-10 combo).

---

### 1. Phân hoạch lớp tương đương (Equivalence Partitioning)

| Trường dữ liệu | Lớp hợp lệ (Valid) | Lớp không hợp lệ (Invalid) |
| :--- | :--- | :--- |
| **Seat Quantity** | Số lượng từ 1 đến 6 ghế | Chọn 0 ghế <br> Chọn trên 6 ghế |
| **Combo Quantity** | Tổng số lượng từ 0 đến 10 | Số lượng âm (< 0) <br> Tổng số lượng trên 10 |

---

### 2. Bảng phân tích giá trị biên (Boundary Value Analysis)

| Trường dữ liệu | Biên dưới (Min) | Biên trên (Max) | Các giá trị biên cần test |
| :--- | :--- | :--- | :--- |
| **Seat Quantity** | 1 | 6 | 0, 1, 2, 5, 6, 7 |
| **Combo Quantity**| 0 | 10 | -1, 0, 1, 9, 10, 11 |

---

### 3. Bảng Test Case chi tiết và Kết quả thực thi

| Test Case ID | Tags | Mô tả Kịch bản | Dữ liệu đầu vào (Input) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TC_BOOKING_01` | `@BVA`, `@Min-1` | Đặt vé với số lượng ghế dưới mức tối thiểu (0 ghế) | `0 ghế` | Nút "Tiếp tục" bị vô hiệu hóa (Disabled) | Nút "Tiếp tục" bị vô hiệu hóa | **PASS** |
| `TC_BOOKING_02` | `@BVA`, `@Min` | Đặt vé với số lượng ghế tại mốc tối thiểu (1 ghế) | `1 ghế` | Cho phép sang bước thanh toán | Cho phép sang bước thanh toán | **PASS** |
| `TC_BOOKING_03` | `@BVA`, `@Max` | Đặt vé với số lượng ghế tại mốc tối đa (6 ghế) | `6 ghế` | Cho phép sang bước thanh toán | Hệ thống báo lỗi vượt quá số lượng cho phép (do logic Backend sai) | **FAIL** (Đã fix) |
| `TC_BOOKING_04` | `@BVA`, `@Max+1` | Đặt vé với số lượng ghế vượt mốc tối đa (7 ghế) | `7 ghế` | Báo lỗi giới hạn 6 ghế, không cho chọn thêm | Báo lỗi giới hạn 6 ghế, không cho chọn thêm | **PASS** |
| `TC_BOOKING_05` | `@BVA`, `@Min` | Không chọn combo nào (0 combo) | `0 combo` | Tổng tiền chỉ tính tiền ghế, cho phép tiếp tục | Tổng tiền chỉ tính tiền ghế, cho phép tiếp tục | **PASS** |
| `TC_BOOKING_06` | `@BVA`, `@Max` | Chọn tổng số lượng combo tại mốc tối đa (10 combo) | `6 Combo A + 4 Combo B` | Cập nhật đúng tổng tiền, cho phép tiếp tục | Cập nhật đúng tổng tiền, cho phép tiếp tục | **PASS** |
| `TC_BOOKING_07` | `@BVA`, `@Max+1` | Chọn tổng số lượng combo vượt mốc tối đa (11 combo) | `Thêm 1 combo khi đã đạt 10` | Các nút cộng (+) combo bị vô hiệu hóa (Disabled) | Các nút cộng (+) bị vô hiệu hóa | **PASS** |