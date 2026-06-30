# PHỤ LỤC A: BÁO CÁO PHÂN TÍCH GIÁ TRỊ BIÊN (BVA) - TÍNH NĂNG ĐĂNG KÝ
**Module:** Authentication (Đăng ký tài khoản mới)
**Người thực hiện:** Nguyễn Minh Thắng
**Mục tiêu:** Kiểm thử các ràng buộc giới hạn đối với trường dữ liệu Password (độ dài 6-20 ký tự) và FullName (độ dài 2-50 ký tự).

---

### 1. Phân hoạch lớp tương đương (Equivalence Partitioning)

| Trường dữ liệu | Lớp hợp lệ (Valid) | Lớp không hợp lệ (Invalid) |
| :--- | :--- | :--- |
| **Password** | Độ dài từ 6 đến 20 ký tự | Dưới 6 ký tự <br> Trên 20 ký tự <br> Bỏ trống (Null) |
| **FullName** | Độ dài từ 2 đến 50 ký tự | Dưới 2 ký tự <br> Trên 50 ký tự <br> Bỏ trống (Null/Ký tự đặc biệt) |

---

### 2. Bảng phân tích giá trị biên (Boundary Value Analysis)

| Trường dữ liệu | Biên dưới (Min) | Biên trên (Max) | Các giá trị biên cần test (Min-1, Min, Min+1, Max-1, Max, Max+1) |
| :--- | :--- | :--- | :--- |
| **Password** | 6 | 20 | 5, 6, 7, 19, 20, 21 |
| **FullName** | 2 | 50 | 1, 2, 3, 49, 50, 51 |

---

### 3. Bảng Test Case chi tiết và Kết quả thực thi

| Test Case ID | Tags | Mô tả Kịch bản | Dữ liệu đầu vào (Input) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TC_AUTH_01` | `@BVA`, `@Min-1` | Đăng ký với Password dưới mức tối thiểu (5 ký tự) | `pass1` | Báo lỗi: "Mật khẩu phải từ 6-20 ký tự" | Báo lỗi: "Mật khẩu phải từ 6-20 ký tự" | **PASS** |
| `TC_AUTH_02` | `@BVA`, `@Min` | Đăng ký với Password tại mốc tối thiểu (6 ký tự) | `pass12` | Đăng ký thành công | Trả về lỗi độ dài (Lỗi Backend cấu hình sai lớn hơn 6) | **FAIL** (Đang fix) |
| `TC_AUTH_03` | `@BVA`, `@Max` | Đăng ký với Password tại mốc tối đa (20 ký tự) | `password123456789012` | Đăng ký thành công | Đăng ký thành công | **PASS** |
| `TC_AUTH_04` | `@BVA`, `@Max+1` | Đăng ký với Password vượt mốc tối đa (21 ký tự) | `password1234567890123` | Báo lỗi: "Mật khẩu vượt quá 20 ký tự" | Báo lỗi: "Mật khẩu vượt quá 20 ký tự" | **PASS** |
| `TC_AUTH_05` | `@BVA`, `@Min-1` | Đăng ký với FullName dưới mức tối thiểu (1 ký tự) | `A` | Báo lỗi: "Họ tên phải từ 2-50 ký tự" | Báo lỗi: "Họ tên phải từ 2-50 ký tự" | **PASS** |
| `TC_AUTH_06` | `@BVA`, `@Min` | Đăng ký với FullName tại mốc tối thiểu (2 ký tự) | `An` | Đăng ký thành công | Đăng ký thành công | **PASS** |
| `TC_AUTH_07` | `@BVA`, `@Max` | Đăng ký với FullName tại mốc tối đa (50 ký tự) | (Chuỗi ngẫu nhiên đúng 50 ký tự) | Đăng ký thành công | Đăng ký thành công | **PASS** |
| `TC_AUTH_08` | `@BVA`, `@Max+1` | Đăng ký với FullName vượt mốc tối đa (51 ký tự) | (Chuỗi ngẫu nhiên đúng 51 ký tự) | Báo lỗi: "Họ tên vượt quá 50 ký tự" | Báo lỗi: "Họ tên vượt quá 50 ký tự" | **PASS** |