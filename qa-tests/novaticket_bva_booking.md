### Phụ lục C: Báo cáo chi tiết BVA - Đặt vé

#### 1. Mô tả bài toán
Hệ thống NovaTicket quy định giới hạn số lượng ghế và combo bắp nước tối đa cho mỗi lần đặt vé để tránh spam và đầu cơ. Yêu cầu đặt vé hợp lệ khi thỏa mãn các điều kiện:

| Biến đầu vào | Ý nghĩa | Kiểu dữ liệu | Miền giá trị hợp lệ |
| :--- | :--- | :--- | :--- |
| `showtimeSeatIds` | Số lượng ghế mà khách hàng chọn | Mảng (Array) | Từ 1 đến 6 ghế |
| `combos` | Tổng số lượng combo bắp nước khách chọn | Mảng đối tượng | Từ 0 đến 8 combo |

#### 2. Xác định lớp tương đương
| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
| :--- | :--- | :--- | :--- | :--- |
| Hạn mức Ghế | 1 <= Số ghế <= 6 | V4 | Số ghế = 0 (Trống)<br>Số ghế > 6 | X5<br>X6 |
| Hạn mức Combo | 0 <= Tổng combo <= 8 | V5 | Tổng combo < 0<br>Tổng combo > 8 | X7<br>X8 |

#### 3. Bảng phân tích giá trị biên
| Biến đầu vào | min | min+ | nominal | max- | max | Tag biên |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Hạn mức Ghế | 1 | 2 | 3 | 5 | 6 | B16(min), B17(min+), B18(nominal), B19(max-), B20(max) |
| Hạn mức Combo | 0 | 1 | 4 | 7 | 8 | B21(min), B22(min+), B23(nominal), B24(max-), B25(max) |

#### 4. Bảng Test Case chi tiết và Kết quả thực thi
| STT | Tên Test Case | Dữ liệu đầu vào (Input mô phỏng) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái | Tag bao phủ |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | BVA_SEAT_01: Biên ghế hợp lệ lớn nhất [6 ghế] | `showtimeSeatIds: {{bva_6_seats}}` | API phản hồi hợp lệ. | ✅ Không bị chặn bởi giới hạn 6 ghế | PASS | V4, B20 |
| 2 | BVA_SEAT_02: Vượt biên ghế không hợp lệ đầu tiên [7 ghế] | `showtimeSeatIds: {{bva_7_seats}}` | Bị chặn lỗi 400 Bad Request. | ✅ Kích hoạt chặn vượt biên 7 ghế - Trả về lỗi 400 | PASS | X6 |
| 3 | BVA_COMBO_01: Biên combo hợp lệ lớn nhất [8 combo] | `combos: [{ qty: 5 }, { qty: 3 }]` | API phản hồi hợp lệ. | ✅ Không bị chặn bởi giới hạn 8 combo | PASS | V5, B25 |
| 4 | BVA_COMBO_02: Vượt biên combo không hợp lệ đầu tiên [9 combo] | `combos: [{ qty: 5 }, { qty: 4 }]` | Bị chặn lỗi 400 Bad Request. | ✅ Kích hoạt chặn vượt biên 9 combo - Trả về lỗi 400 | PASS | X8 |