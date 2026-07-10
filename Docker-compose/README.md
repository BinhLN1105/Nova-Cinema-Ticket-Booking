# Hướng Dẫn Chạy Dự Án NovaTicket Bằng Docker Compose

Tài liệu này hướng dẫn cách khởi chạy nhanh toàn bộ dự án NovaTicket (bao gồm Frontend, Backend, Database và Cache) trên môi trường local bằng Docker Compose.

---

## 📌 Yêu Cầu Hệ Thống

Trước khi bắt đầu, hãy đảm bảo máy tính đã cài đặt:
* **Docker** và **Docker Compose**
* Trình duyệt web hỗ trợ truy cập cục bộ (Chrome, Edge, Firefox,...)

---

## 🚀 1. Khởi Chạy Hệ Thống

### Bước 1: Thiết lập file môi trường (`.env`)
1. Tạo file `.env` từ file mẫu `env.example`:
   ```bash
   cp env.example .env
   ```
2. Mở file `.env` vừa tạo và điền các giá trị cấu hình cần thiết (như các khóa API thanh toán, Cloudinary, Firebase và Client ID mạng xã hội).

### Bước 2: Khởi chạy các container
Chạy lệnh sau tại thư mục chứa file `docker-compose.yml`:
```bash
docker compose up -d
```
Hệ thống sẽ tải hình ảnh và khởi chạy các dịch vụ trong nền.

---

## 🔒 2. Truy Cập Giao Diện (HTTPS)

Hệ thống Frontend được cấu hình chạy trên giao thức HTTPS bảo mật:
* **Địa chỉ truy cập**: `https://localhost`
* **Chứng chỉ SSL**: Đã được cấu hình tự ký (`localhost.crt` và `localhost.key`) lưu tại thư mục `certs/`.
* *Lưu ý*: Khi truy cập lần đầu trên trình duyệt, hãy chọn **Nâng cao (Advanced)** -> **Tiếp tục truy cập localhost (unsafe)** để bỏ qua cảnh báo bảo mật của trình duyệt đối với chứng chỉ tự ký.

---

## 📧 3. Hộp Thư Giả Lập (MailDev)

Dự án tích hợp công cụ **MailDev** để thu thập và giả lập việc gửi/nhận email (như thông báo đặt vé thành công, mã OTP, hóa đơn):
* **Đường dẫn truy cập hộp thư**: `http://localhost:1080`
* Bạn có thể mở liên kết trên để kiểm tra toàn bộ email được gửi từ hệ thống Backend trong quá trình chạy thử nghiệm mà không cần cấu hình tài khoản SMTP thật.

---

## 🔄 4. Khởi Tạo Cơ Sở Dữ Liệu (Auto-seeding)

Dự án đã được tích hợp sẵn cơ chế tự động nạp dữ liệu mẫu:
* File dữ liệu mẫu **`init.sql`** chứa cấu trúc cơ sở dữ liệu và dữ liệu ban đầu (phim, rạp, bắp nước, lịch chiếu, tài khoản kiểm thử) đã được gắn vào thư mục khởi tạo của PostgreSQL container.
* Khi chạy lệnh `docker compose up -d` lần đầu tiên, hệ thống sẽ **tự động nạp dữ liệu từ `init.sql`** vào cơ sở dữ liệu. Không cần thực hiện thêm bất cứ lệnh SQL thủ công nào.

---

## 🔑 5. Danh Sách Tài Khoản Thử Nghiệm (Test Accounts)

Hệ thống đã tự động khởi tạo sẵn các tài khoản kiểm thử sau để phục vụ việc đánh giá và đăng nhập trực tiếp trên giao diện:

| Vai trò (Role) | Email đăng nhập | Mật khẩu mặc định |
| :--- | :--- | :--- |
| **Quản trị viên (ADMIN)** | `admin_test@novaticket.com` | `AdminPassword123!` |
| **Nhân viên (STAFF)** | `staff_test@novaticket.com` | `StaffPassword123!` |
| **Khách hàng (CUSTOMER)** | `customer_test@novaticket.com` | `CustomerPassword123!` |

*(Lưu ý: Người dùng có thể đăng nhập bằng tài khoản Google cá nhân của mình, sau đó sử dụng lệnh SQL ở phần bên dưới để nâng cấp vai trò của tài khoản).*

---

## 🛠️ 6. Thao Tác Cơ Sở Dữ Liệu Thường Dùng

### Truy cập dòng lệnh PostgreSQL (psql CLI)
Để tương tác trực tiếp với cơ sở dữ liệu bên trong container:
```bash
docker exec -it novaticket-postgres psql -U postgres -d novaticket_prod
```

### Cấp quyền quản trị (ADMIN) cho tài khoản
Để nâng cấp vai trò của một tài khoản sau khi đăng nhập:
```bash
docker exec -it novaticket-postgres psql -U postgres -d novaticket_prod -c "UPDATE users SET role = 'ADMIN' WHERE email = 'email_tai_khoan@gmail.com';"
```

---

## 📞 7. Thông Tin Liên Hệ

Nếu gặp bất kỳ khó khăn nào trong quá trình khởi chạy hoặc cần trao đổi thêm về dự án, vui lòng liên hệ:
* **Tác giả**: Lưu Nhật Bình
* **Email**: [binhluu953348@gmail.com](mailto:binhluu953348@gmail.com)
* **Facebook**: [me](https://www.facebook.com/binh.luunhat)
