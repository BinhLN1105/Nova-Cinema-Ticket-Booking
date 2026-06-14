# Hướng dẫn Phát triển và Chạy UI Functional E2E Testing (Playwright + CodeceptJS)

Thư mục này chứa toàn bộ cấu hình và kịch bản kiểm thử giao diện người dùng tự động (E2E UI Testing) cho ứng dụng NovaTicket (CineNoir).

---

## 📂 1. Cấu trúc Thư mục & Phân chia Nhiệm vụ

Để tránh xung đột code khi gộp nhánh (Git Merge), thư mục test E2E được chia nhỏ theo luồng nghiệp vụ tương ứng với từng thành viên:

```plaintext
tests/e2e/
├── auth/            # [Thành viên 7] Luồng Xác thực (Đăng nhập, Đăng ký, Profile...)
├── admin/           # [Thành viên 5] Luồng Admin (Movie, Cinema, Showtime, Voucher, Dashboard...)
├── customer/        # [Thành viên 4] Luồng Khách hàng (Giữ ghế, Thanh toán ví, Điểm thưởng...)
├── staff/           # [Thành viên 6] Luồng Nhân viên (Soát vé, Quét mã QR...)
├── output/          # Thư mục chứa session logs, test data và screenshots lỗi (được gitignore)
├── bootstrap.cjs    # Script khởi tạo/gieo dữ liệu Newman Seed tự động trước test suite
├── steps_file.js    # Định nghĩa các Custom steps cho đối tượng I
└── skeleton_test.js # File test khung xương để verify môi trường
```

*Lưu ý: Tất cả các file kịch bản test mới phải được đặt trong các thư mục con tương ứng ở trên và đặt tên theo định dạng `*_test.js`.*

---

## 🚀 2. Hướng dẫn Chạy Test ở Local

### Bước 1: Cài đặt thư viện (Chỉ thực hiện một lần)
Di chuyển vào thư mục Frontend và cài đặt dependencies:
```bash
cd Frontend/nova-ticketbooking
npm install --legacy-peer-deps
```

### Bước 2: Chuẩn bị Môi trường
1. Khởi chạy **Database** (PostgreSQL & Redis).
2. Khởi chạy **Spring Boot Backend** ở cổng `8080` (Dùng profile `test` để tự động tạo tài khoản Admin, Staff, Customer ảo).
3. Khởi chạy **React Frontend** ở cổng `5173` (Giao thức HTTPS).

### Bước 3: Cấu hình biến môi trường E2E (Nếu file codecept.conf.js không có fallback)
Tạo file `.env` tại `Frontend/nova-ticketbooking/.env` nếu máy của bạn chưa cấu hình mật khẩu test:
```env
APP_TEST_ADMIN_PASSWORD=AdminPassword123!
APP_TEST_STAFF_PASSWORD=StaffPassword123!
APP_TEST_CUSTOMER_PASSWORD=CustomerPassword123!
```

### Bước 4: Chạy test
Chạy toàn bộ kịch bản test và in ra từng bước thực hiện:
```bash
npx codeceptjs run --steps
```
Nếu muốn chạy một file test cụ thể:
```bash
npx codeceptjs run tests/e2e/skeleton_test.js --steps
```

---

## 🔐 3. Cơ chế Đăng nhập tự động (`loginAs`)

Để tăng tốc độ test (giảm thời gian test xuống $<0.5\text{s}$ cho các bước sau), hệ thống sử dụng cơ chế lưu session (Cookies/LocalStorage) thông qua plugin `auth` của CodeceptJS.

Khi viết kịch bản test yêu cầu quyền, bạn **không cần viết code điền form đăng nhập**. Hãy gọi trực tiếp hàm `loginAs(<role>)`:

```javascript
Feature('Trang quản lý phim');

// Sử dụng loginAs('admin') để tự động nạp session của admin
Scenario('Admin thêm phim mới', ({ I, loginAs }) => {
  loginAs('admin');
  I.amOnPage('/admin/movies');
  I.see('Quản lý phim');
  // Thực hiện các bước test tiếp theo...
});
```

Các quyền được hỗ trợ:
*   `loginAs('admin')`: Truy cập `/admin/dashboard`
*   `loginAs('staff')`: Truy cập `/staff/dashboard`
*   `loginAs('customer')`: Truy cập `/profile`

*Lưu ý: Session sẽ được lưu tạm thời vào thư mục `tests/e2e/output/session_<role>.json` (được ignore tự động trong git).*

---

## 🧱 4. Cơ chế Đọc dữ liệu dùng chung (`test-data.json`)

Khi bộ kiểm thử bắt đầu chạy, script `bootstrap.cjs` sẽ tự động chạy Newman Seed Collection của Thành viên 3 để gieo dữ liệu động vào Database và xuất ra file cấu hình dùng chung tại địa chỉ:
`Frontend/nova-ticketbooking/tests/e2e/output/test-data.json`.

Để viết test có sử dụng các dữ liệu nền động (ID phim, ID suất chiếu, mã voucher...), các thành viên **bắt buộc phải import file test-data.json này thay vì hardcode giá trị**:

```javascript
const testData = require('./output/test-data.json');

Feature('Khách hàng mua vé');

Scenario('Khách hàng chọn ghế và mua vé', ({ I, loginAs }) => {
  loginAs('customer');
  
  // Trỏ trực tiếp đến ID suất chiếu động đã được gieo từ database
  I.amOnPage(`/booking/showtime/${testData.showtime_id}`);
  
  // Ví dụ: Chọn ghế...
});
```

### Cấu trúc dữ liệu trong `test-data.json`:
```json
{
  "movie_id": "UUID của phim mẫu",
  "movie_name": "Tên phim mẫu",
  "showtime_id": "UUID của suất chiếu mẫu",
  "cinema_id": "Mã phòng chiếu/rạp mẫu",
  "customer_wallet_balance": 2000000,
  "customer_reward_points": 2000,
  "staff_checkin_code": "Mã vé mẫu dùng để soát vé",
  "cancel_test_booking_code": "Mã vé mẫu dùng để hủy vé",
  "voucher_code_valid": "Mã voucher hợp lệ",
  "voucher_code_expired": "Mã voucher hết hạn"
}
```

---

## 🧹 5. Quy tắc cô lập dữ liệu (Test Isolation Rule)

1. **Không chỉnh sửa/xóa dữ liệu dùng chung**: Các dữ liệu mẫu trong `test-data.json` là tài nguyên dùng chung của cả suite. Nghiêm cấm xóa hoặc sửa các bản ghi này.
2. **Tự dọn dẹp dữ liệu động**: Nếu kịch bản test của bạn có tạo thêm dữ liệu động (như tạo voucher mới, tạo phim mới, tạo phòng chiếu mới...), bạn phải viết logic xóa/dọn dẹp dữ liệu đó trong hook `After()` để đảm bảo môi trường sạch cho các lần chạy sau.
3. **Chụp ảnh màn hình khi lỗi**: Môi trường CI/CD đã tự động cấu hình chụp ảnh màn hình và lưu vào `tests/e2e/output/` khi test bị thất bại để hỗ trợ debug.
