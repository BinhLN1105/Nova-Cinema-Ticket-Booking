# PHỤ LỤC B: BÁO CÁO PHÂN TÍCH GIÁ TRỊ BIÊN (BVA) - TÍNH NĂNG ĐÁNH GIÁ PHIM
**Module:** Review (Đánh giá phim)
**Người thực hiện:** Nguyễn Minh Thắng
**Mục tiêu:** Kiểm thử giới hạn đối với trường dữ liệu Điểm đánh giá (Rating Score: số nguyên từ 1 đến 5).

---

### 1. Phân hoạch lớp tương đương (Equivalence Partitioning)

| Trường dữ liệu | Lớp hợp lệ (Valid) | Lớp không hợp lệ (Invalid) |
| :--- | :--- | :--- |
| **Rating Score** | Số nguyên từ 1 đến 5 | Nhỏ hơn 1 <br> Lớn hơn 5 <br> Số thập phân (ví dụ: 4.5) |

---

### 2. Bảng phân tích giá trị biên (Boundary Value Analysis)

| Trường dữ liệu | Biên dưới (Min) | Biên trên (Max) | Các giá trị biên cần test |
| :--- | :--- | :--- | :--- |
| **Rating Score** | 1 | 5 | 0, 1, 2, 4, 5, 6, 4.5 (thập phân) |

---

### 3. Bảng Test Case chi tiết và Kết quả thực thi

| Test Case ID | Tags | Mô tả Kịch bản | Dữ liệu đầu vào (Input) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TC_RATING_01` | `@BVA`, `@Min-1` | Đánh giá với số điểm dưới mức tối thiểu (0 sao) | `0` | Báo lỗi: "Điểm đánh giá phải từ 1 đến 5" | Báo lỗi: "Điểm đánh giá phải từ 1 đến 5" | **PASS** |
| `TC_RATING_02` | `@BVA`, `@Min` | Đánh giá với số điểm tại mốc tối thiểu (1 sao) | `1` | Đánh giá thành công | Đánh giá thành công | **PASS** |
| `TC_RATING_03` | `@BVA`, `@Max` | Đánh giá với số điểm tại mốc tối đa (5 sao) | `5` | Đánh giá thành công | Đánh giá thành công | **PASS** |
| `TC_RATING_04` | `@BVA`, `@Max+1` | Đánh giá với số điểm vượt mốc tối đa (6 sao) | `6` | Báo lỗi: "Điểm đánh giá phải từ 1 đến 5" | Báo lỗi: "Điểm đánh giá phải từ 1 đến 5" | **PASS** |
| `TC_RATING_05` | `@BVA`, `@Invalid` | Đánh giá với số điểm thập phân (4.5 sao) | `4.5` | Báo lỗi: "Điểm đánh giá phải là số nguyên" | Hệ thống vẫn lưu điểm số thập phân vào CSDL làm sai lệch hiển thị | **FAIL** (Đã fix) |
| `TC_RATING_06` | `@BVA`, `@Invalid` | Đánh giá với số điểm thập phân (1.5 sao) | `1.5` | Báo lỗi: "Điểm đánh giá phải là số nguyên" | Báo lỗi: "Điểm đánh giá phải là số nguyên" (sau khi fix) | **PASS** |