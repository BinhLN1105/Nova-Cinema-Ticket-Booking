# Hướng dẫn kiểm thử giao diện tự động (UI Testing) với CodeceptJS và Playwright

Tài liệu này hướng dẫn cách phát triển, chạy và quản lý các kịch bản kiểm thử giao diện người dùng tự động (E2E UI Testing) cho ứng dụng **NovaTicket** sử dụng **CodeceptJS** kết hợp thư viện **Playwright**.

---

## 📂 1. Cấu trúc thư mục E2E UI Testing

Thư mục test E2E nằm tại `Frontend/nova-ticketbooking/tests/e2e/`. Để hạn chế xung đột code khi gộp nhánh Git (Git Merge Conflict), mã nguồn test được phân chia rõ ràng theo phân hệ:

```plaintext
tests/e2e/
├── auth/            # Luồng Xác thực (Đăng nhập, Đăng ký, Profile...)
├── admin/           # Luồng Admin (Movie, Cinema, Showtime, Voucher...)
├── customer/        # Luồng Khách hàng (Chọn ghế, Áp dụng Voucher, Đặt vé...)
├── staff/           # Luồng Nhân viên (Soát vé, Check-in QR...)
├── output/          # Chứa ảnh chụp màn hình khi test lỗi (Screenshots) và Session cookies (Được gitignore)
├── bootstrap.cjs    # Script tự động gieo dữ liệu động vào database trước khi test suite bắt đầu
├── steps_file.js    # Định nghĩa các Custom Steps mở rộng cho đối tượng I
└── skeleton_test.js # File test khung xương dùng để xác minh môi trường chạy
```

---

## 🚀 2. Hướng dẫn khởi chạy Test ở local

### Bước 1: Cài đặt thư viện
Di chuyển vào thư mục Frontend và chạy lệnh cài đặt (chỉ thực hiện lần đầu):
```bash
cd Frontend/nova-ticketbooking
npm install --legacy-peer-deps
```

### Bước 2: Chuẩn bị máy chủ chạy thật
Đảm bảo tất cả các thành phần sau đang chạy ổn định ở máy local:
1.  **PostgreSQL & Redis** (Cơ sở dữ liệu & bộ nhớ đệm).
2.  **Spring Boot Backend** đang chạy tại cổng `8080` (sử dụng profile `test` để hệ thống tự tạo các tài khoản ảo).
3.  **Vite React Frontend** đang chạy tại cổng `5173` (giao thức HTTPS).

### Bước 3: Lệnh chạy test
*   **Chạy toàn bộ các kịch bản test và in ra các bước chi tiết:**
    ```bash
    npx codeceptjs run --steps
    ```
*   **Chạy một file test cụ thể:**
    ```bash
    npx codeceptjs run tests/e2e/skeleton_test.js --steps
    ```
*   **Chạy test với giao diện đồ họa (UI Mode):**
    ```bash
    npx codeceptjs run --ui
    ```

---

## 🔐 3. Cơ chế nâng cao: Đăng nhập tự động (`loginAs`)

Để tránh việc lặp lại các bước điền form đăng nhập tốn thời gian ($3\text{s} - 5\text{s}$ mỗi lần), dự án sử dụng plugin lưu phiên đăng nhập (Cookies/LocalStorage) thông qua custom function `loginAs`.

Khi viết kịch bản test yêu cầu quyền, bạn **không cần viết code điền form đăng nhập**. Hãy gọi trực tiếp hàm `loginAs(<role>)`:

```javascript
Feature('Trang quản lý phim của Admin');

Scenario('Admin thêm phim mới thành công', ({ I, loginAs }) => {
  loginAs('admin'); // Tự động nạp session của tài khoản admin mẫu
  I.amOnPage('/admin/movies');
  I.see('Quản lý phim');
  // Thực hiện các bước click, fillField tiếp theo...
});
```

Các quyền/vai trò được hỗ trợ trong hệ thống:
*   `loginAs('admin')`: Đăng nhập bằng tài khoản Admin.
*   `loginAs('staff')`: Đăng nhập bằng tài khoản Staff (nhân viên).
*   `loginAs('customer')`: Đăng nhập bằng tài khoản Customer (khách hàng).

---

## 🧱 4. Sử dụng dữ liệu động dùng chung (`test-data.json`)

Khi test suite bắt đầu chạy, script `bootstrap.cjs` sẽ tự động chạy Newman Seed để tạo dữ liệu ngẫu nhiên trong DB và xuất ra file cấu hình dùng chung tại `tests/e2e/output/test-data.json`.

Để tránh lỗi dữ liệu bị lỗi thời (Stale data), lập trình viên **bắt buộc phải import file test-data.json để lấy ID động** thay vì hardcode giá trị:

```javascript
const testData = require('./output/test-data.json');

Feature('Khách hàng mua vé');

Scenario('Khách hàng chọn ghế và mua vé', ({ I, loginAs }) => {
  loginAs('customer');
  // Truy cập trang đặt vé bằng ID suất chiếu động đã được gieo trong DB
  I.amOnPage(`/booking/showtime/${testData.showtime_id}`);
  I.click('Ghế A-12');
  I.click('Thanh toán');
  I.see('Xác nhận thông tin đặt vé');
});
```

---

## 🛠️ 5. Các câu lệnh CodeceptJS tương tác UI phổ biến

Khi viết kịch bản test mới, sử dụng đối tượng `I` để thực hiện các thao tác:

*   **Điều hướng trang:** `I.amOnPage('/path')`
*   **Click chuột:** `I.click('Tên nút hoặc CSS selector')`
*   **Nhập dữ liệu:** `I.fillField('Tên trường hoặc CSS selector', 'Nội dung nhập')`
*   **Kiểm tra giao diện:**
    *   `I.see('Nội dung cần thấy trên màn hình')`
    *   `I.dontSee('Nội dung không được xuất hiện')`
    *   `I.seeElement('CSS selector')`
*   **Chờ đợi:** 
    *   `I.wait(2)` (Chờ cứng 2 giây - Hạn chế dùng).
    *   `I.waitForElement('CSS selector', 5)` (Chờ tối đa 5 giây cho đến khi element xuất hiện trên DOM - Khuyên dùng để tăng tốc độ test).
*   **Quy tắc cô lập dữ liệu (Test Isolation):** Nếu kịch bản test của bạn có tạo thêm dữ liệu động (như tạo voucher, tạo phim...), bạn phải viết hook `After()` để tự động gọi API xóa/dọn dẹp dữ liệu đó, trả lại môi trường sạch cho các kịch bản test phía sau.
