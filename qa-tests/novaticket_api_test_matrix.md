# 🧪 NOVATICKET: API TEST CASE MATRIX & STATUS CODE SPECIFICATION

Tài liệu này quy định **Bộ Quy Ước HTTP Status Code** và **Ma Trận Kịch Bản Kiểm Thử (Test Case Matrix)** chuẩn RESTful áp dụng bắt buộc cho toàn bộ đội ngũ phát triển (Dev Backend/Frontend) và đội ngũ kiểm thử (QA/QC) của dự án NovaTicket.

---

## 1. BẢNG QUY ƯỚC MÃ TRẢ VỀ (HTTP STATUS CODE SYSTEM)

Để đảm bảo tính đồng bộ giữa các thành viên, toàn bộ API của NovaTicket bắt buộc tuân theo bảng luật phân loại mã lỗi dưới đây:

| Mã HTTP | Trạng thái (Status) | Phân nhóm màu | Mô tả nghiệp vụ thực tế | Phản ứng của QA |
| :--- | :--- | :---: | :--- | :--- |
| **`200`** | **`OK`** | 🟢 XANH LÁ | Yêu cầu thành công. Thường dùng cho các API lấy dữ liệu (`GET`), cập nhật (`PUT`/`PATCH`), hoặc Đăng nhập (`POST`). | Đạt (Pass) |
| **`201`** | **`Created`** | 🟢 XANH LÁ | Tạo mới thành công. Bắt buộc dùng khi sinh ra thực thể dữ liệu mới (Đăng ký tài khoản, Tạo đơn vé, Thêm phim). | Đạt (Pass) |
| **`204`** | **`No Content`** | 🟢 XANH LÁ | Thực thi thành công và không cần trả về dữ liệu gì thêm. Thường dùng cho API xóa thực thể (`DELETE`). | Đạt (Pass) |
| **`400`** | **`Bad Request`** | 🟡 VÀNG | Dữ liệu Client gửi lên bị sai định dạng, thiếu trường bắt buộc, hoặc không vượt qua bộ lọc validate (ví dụ: pass yếu). | Đạt (Nếu kịch bản test sai) |
| **`401`** | **`Unauthorized`** | 🟡 VÀNG | Lỗi xác thực. Token hết hạn, không có token, hoặc token không hợp lệ khi gọi API Private. | Đạt (Nếu test bảo mật) |
| **`403`** | **`Forbidden`** | 🟡 VÀNG | Lỗi phân quyền. Người dùng đã đăng nhập thành công nhưng không đủ quyền hạn truy cập (CUSTOMER gọi API của ADMIN). | Đạt (Nếu test bảo mật) |
| **`404`** | **`Not Found`** | 🟡 VÀNG | Không tìm thấy tài nguyên. Truy cập ID phim, rạp hoặc đơn đặt vé không tồn tại trong hệ thống. | Đạt (Nếu test lỗi) |
| **`409`** | **`Conflict`** | 🟡 VÀNG | Xung đột nghiệp vụ. Đặt trùng ghế, đăng ký trùng Email đã tồn tại, áp dụng Voucher đã hết lượt sử dụng. | Đạt (Nếu test nghiệp vụ) |
| **`500`** | **`Internal Error`** | 🔴 ĐỎ | **CỰC KỲ NGUY HIỂM!** Crash code Backend, lỗi kết nối DB, lỗi logic không được catch. | **LẬP TỨC LOG BUG JIRA!** |

---

## 2. PHÂN BỔ THÀNH VIÊN & MA TRẬN 3 LUỒNG CHI TIẾT

Mỗi thành viên khi tạo thư mục trên Postman Workspace bắt buộc phải nhân bản (Duplicate) mỗi API thành **ít nhất 3 kịch bản kiểm thử** (Happy Path, Negative Path, Edge Cases/Security) như đặc tả dưới đây:

---

### 🧑‍💻 1. TUẤN VÕ — PHÂN HỆ: XÁC THỰC & BẢO MẬT (AUTH & SECURITY)

#### 🔑 API: Đăng nhập hệ thống (`POST /api/v1/auth/login`)
* **Kịch bản 1: `[200] Đăng nhập thành công (Happy Path)`**
  * *Request*: Nhập đúng email (`testmember@gmail.com`) + đúng password (`Password123!`).
  * *Expect Response*: Code `200 OK`. Trả về `accessToken` và `refreshToken`.
  * *Postman Test Script*:
    ```javascript
    pm.test("Đăng nhập thành công (Status 200)", function () { pm.response.to.have.status(200); });
    pm.test("Có chứa AccessToken", function () {
        var jsonData = pm.response.json();
        pm.expect(jsonData.data.accessToken).to.not.be.undefined;
    });
    ```
* **Kịch bản 2: `[400] Đăng nhập sai mật khẩu (Negative)`**
  * *Request*: Nhập đúng email + sai mật khẩu (`wrongpass`).
  * *Expect Response*: Code `400 Bad Request` hoặc `401 Unauthorized`.
  * *Postman Test Script*:
    ```javascript
    pm.test("Bị chặn đúng logic (Status 400 hoặc 401)", function () {
        pm.expect(pm.response.code).to.be.oneOf([400, 401]);
    });
    ```
* **Kịch bản 3: `[400] Đăng nhập bỏ trống Email (Negative)`**
  * *Request*: Email để trống + Password nhập đúng.
  * *Expect Response*: Code `400 Bad Request` kèm thông báo "Email không được để trống".

---

### 🧑‍💻 2. TRINM3962 — PHÂN HỆ: PHIM & LỊCH CHIẾU (MOVIES & SHOWTIMES)

#### 🎬 API: Thêm phim mới (`POST /api/v1/movies`)
* **Kịch bản 1: `[201] Thêm phim thành công (Happy Path)`**
  * *Auth*: Role `ADMIN` (Bearer Token hợp lệ).
  * *Request*: Dữ liệu phim đầy đủ, đúng định dạng.
  * *Expect Response*: Code `201 Created` + Object chi tiết phim mới.
* **Kịch bản 2: `[403] Chặn khách hàng cố ý thêm phim (Security)`**
  * *Auth*: Role `CUSTOMER` (Bearer Token của khách hàng thường).
  * *Expect Response*: Code `403 Forbidden` chặn không cho tạo.
  * *Postman Test Script*:
    ```javascript
    pm.test("Quyền CUSTOMER bị cấm (Status 403)", function () { pm.response.to.have.status(403); });
    ```
* **Kịch bản 3: `[400] Thêm phim thiếu trường bắt buộc (Negative)`**
  * *Auth*: Role `ADMIN`.
  * *Request*: Bỏ trống trường `title` hoặc `duration = -10` (âm).
  * *Expect Response*: Code `400 Bad Request` chặn dữ liệu rác.

---

### 🧑‍💻 3. BÌNH — PHÂN HỆ: RẠP, PHÒNG & SƠ ĐỒ GHẾ (CINEMAS & LAYOUT)

#### 🏛️ API: Lưu sơ đồ ghế Seat Builder (`PUT /api/v1/cinemas/.../screens/.../seats`)
* **Kịch bản 1: `[200] Lưu sơ đồ thành công (Happy Path)`**
  * *Request*: Gửi ma trận ghế gồm hàng, cột và phân loại Standard, VIP hợp lý.
  * *Expect Response*: Code `200 OK` hoặc `204 No Content`.
* **Kịch bản 2: `[400] Lưu sơ đồ tọa độ hàng cột âm (Negative)`**
  * *Request*: Gửi tọa độ ghế `gridRow: -5`, `gridCol: -2`.
  * *Expect Response*: Code `400 Bad Request` từ chối lưu.
* **Kịch bản 3: `[409] Lưu sơ đồ trùng tọa độ ghế (Edge Case)`**
  * *Request*: Hai ghế cùng nằm trên 1 tọa độ hàng 1 cột 1.
  * *Expect Response*: Code `409 Conflict` báo lỗi trùng lắp vị trí.

---

### 🧑‍💻 4. THANGVN0987 — PHÂN HỆ: ĐẶT VÉ & CHECK-IN (BOOKING & CHECK-IN)

#### 🎫 API: Tạo đơn đặt vé & khóa ghế (`POST /api/v1/bookings`)
* **Kịch bản 1: `[201] Khóa ghế thành công (Happy Path)`**
  * *Request*: Ghế còn trống (`AVAILABLE`) + ID suất chiếu hợp lệ.
  * *Expect Response*: Code `201 Created`, trả về thông tin hóa đơn và kích hoạt bộ đếm giữ ghế.
* **Kịch bản 2: `[409] Chặn trùng khi khóa ghế đang giữ (Edge Case)`**
  * *Request*: Khách hàng cố đặt lại đúng mã ghế đã bị khóa bởi người khác trước đó.
  * *Expect Response*: Code `409 Conflict` báo lỗi "Ghế đã được chọn hoặc đang được thanh toán".
  * *Postman Test Script*:
    ```javascript
    pm.test("Tranh chấp ghế báo lỗi 409", function () { pm.response.to.have.status(409); });
    ```
* **Kịch bản 3: `[404] Đặt vé Suất chiếu không tồn tại (Negative)`**
  * *Request*: Gửi ID suất chiếu ảo (`UUID` không tồn tại trong DB).
  * *Expect Response*: Code `404 Not Found` báo lỗi không tìm thấy showtime.

---

### 🧑‍💻 5. LƯU MINH TRIẾT — PHÂN HỆ: THANH TOÁN & VÍ (PAYMENTS & WALLET)

#### 💳 API: Thanh toán buyout bằng ví CinePoint (`POST /api/v1/payments/wallet/{bookingId}`)
* **Kịch bản 1: `[200] Thanh toán ví thành công (Happy Path)`**
  * *Request*: Số dư ví CP >= giá trị đơn hàng.
  * *Expect Response*: Code `200 OK`, trạng thái đơn vé chuyển sang `PAID` ngay lập tức.
* **Kịch bản 2: `[409] Số dư ví tích lũy không đủ (Edge Case)`**
  * *Request*: Giá vé 120.000đ nhưng ví chỉ còn 50 CinePoint (50.000đ).
  * *Expect Response*: Code `409 Conflict` báo lỗi "Số dư ví không đủ".
* **Kịch bản 3: `[409] Thanh toán trùng lặp hóa đơn đã trả (Security)`**
  * *Request*: Hóa đơn đã có trạng thái `PAID` nhưng cố tình gọi lại API ví.
  * *Expect Response*: Code `409 Conflict` chặn thanh toán trùng lặp.

---

### 🧑‍💻 6. NGUYÊN VŨ — PHÂN HỆ: TIỆN ÍCH MỞ RỘNG (UTILITIES)

#### 🍿 API: Viết bình luận đánh giá phim (`POST /api/v1/reviews`)
* **Kịch bản 1: `[201] Viết đánh giá thành công (Happy Path)`**
  * *Condition*: Người dùng đã đi xem phim thực tế (vé có trạng thái `CHECKED_IN`).
  * *Request*: Nhập 5 sao kèm bình luận văn minh.
  * *Expect Response*: Code `201 Created` ghi nhận đánh giá vào DB.
* **Kịch bản 2: `[409] Chặn review ảo khi chưa đi xem (Security)`**
  * *Condition*: Người dùng chưa từng mua vé xem phim này, hoặc vé mua chưa được check-in.
  * *Request*: Gọi API gửi review.
  * *Expect Response*: Code `400 Bad Request` hoặc `409 Conflict` chặn review ảo.
  * *Postman Test Script*:
    ```javascript
    pm.test("Spam review bị chặn", function () {
        pm.expect(pm.response.code).to.be.oneOf([400, 409]);
    });
    ```
* **Kịch bản 3: `[400] Đánh giá phim vượt quá 5 sao (Negative)`**
  * *Request*: Gửi dữ liệu đánh giá `11 sao`.
  * *Expect Response*: Code `400 Bad Request` báo lỗi dữ liệu nhập không hợp lệ.

---

## 3. MẸO SỬ DỤNG DYNAMIC VARIABLES TRONG POSTMAN

Để khi các bạn QA chạy test suite lặp đi lặp lại ở cả **Local** lẫn **CI/CD** mà không bao giờ bị trùng lặp dữ liệu (gây ra lỗi trùng Email `409` giả), hãy sử dụng **Pre-request Script** sinh dữ liệu ngẫu nhiên của Postman:

1. Tại request `Đăng ký`, chọn tab **Pre-request Script** và dán đoạn mã sau:
```javascript
// Tạo một chuỗi email ngẫu nhiên dựa vào timestamp thời gian thực
const randomEmail = "member_" + Date.now() + "@gmail.com";
// Tạo một SĐT ngẫu nhiên để tránh trùng
const randomPhone = "09" + Math.floor(10000000 + Math.random() * 90000000);

// Lưu vào biến môi trường tạm thời
pm.environment.set("RegisterEmail", randomEmail);
pm.environment.set("RegisterPhone", randomPhone);
```

2. Tại tab **Body** của Request Đăng ký, dán JSON động này vào:
```json
{
  "email": "{{RegisterEmail}}",
  "password": "Password123!",
  "name": "Nguyễn Văn A",
  "phone": "{{RegisterPhone}}"
}
```

Mỗi lần nhấn **Send**, Postman sẽ tự động sinh email và số điện thoại mới tinh, chạy 1 triệu lần cũng không bao giờ bị trùng dữ liệu cũ!
