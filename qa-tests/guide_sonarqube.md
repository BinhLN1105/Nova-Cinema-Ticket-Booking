# Hướng dẫn chạy và đọc báo cáo SonarQube

Tài liệu này hướng dẫn cách thiết lập, khởi chạy phân tích mã nguồn (Static Code Analysis) bằng SonarQube và cách đọc các chỉ số đo lường chất lượng code của dự án **NovaTicket**.

---

## 📂 1. Chuẩn bị môi trường SonarQube cục bộ

Để chạy phân tích, bạn cần có một máy chủ SonarQube Server hoạt động. Cách nhanh nhất là sử dụng Docker:

### Khởi chạy SonarQube bằng Docker
Chạy lệnh sau trên terminal để dựng nhanh container SonarQube (Community Edition):
```bash
docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest
```

### Đăng nhập và tạo Token
1. Truy cập giao diện quản trị tại: `http://localhost:9000`
2. Đăng nhập với tài khoản mặc định: 
   * **Username:** `admin`
   * **Password:** `admin`
   * *(Hệ thống sẽ yêu cầu bạn đổi mật khẩu trong lần đăng nhập đầu tiên).*
3. Tạo Token bảo mật:
   * Vào **My Account** $\rightarrow$ **Security** $\rightarrow$ **Generate Tokens**.
   * Nhập tên Token (ví dụ: `novaticket_token`) $\rightarrow$ Chọn loại `User Token` $\rightarrow$ Nhấp **Generate**.
   * **Lưu lại Token này** để sử dụng khi chạy maven command.

---

## 🚀 2. Khởi chạy phân tích mã nguồn từ Maven

SonarQube hỗ trợ plugin Maven chính thống giúp quét dự án Spring Boot dễ dàng.

### Cách 1: Chạy trực tiếp từ CMD/PowerShell
Di chuyển đến thư mục backend chứa file `pom.xml` (`Backend/ticket-booking`) và chạy lệnh:
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=novaticket-backend \
  -Dsonar.projectName="NovaTicket Backend" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=TOKEN_CỦA_BẠN
```
*(Thay thế `TOKEN_CỦA_BẠN` bằng token bạn vừa tạo ở Bước 1).*

### Cách 2: Tích hợp cấu hình vào file `.env` hoặc Maven Profile
Bạn có thể cấu hình sẵn các thuộc tính sonar trong `pom.xml` để rút ngắn lệnh chạy chỉ còn `mvn sonar:sonar`.

---

## 📊 3. Cách đọc và giải thích các chỉ số chất lượng (Metrics)

Sau khi quá trình quét hoàn tất, truy cập lại `http://localhost:9000/dashboard?id=novaticket-backend`. Bạn cần quan tâm đến **5 chỉ số cốt lõi (Quality Gates)** sau:

### 1. Bugs (Lỗi logic)
* **Ý nghĩa:** Các đoạn code có khả năng cao gây ra crash, lỗi NullPointerException, hoặc sai lệch tính toán.
* **Hành động:** Cần được sửa chữa ngay lập tức (Critical/Blocker).

### 2. Vulnerabilities & Security Hotspots (Lỗ hổng bảo mật)
* **Ý nghĩa:** Điểm yếu bảo mật (như SQL Injection, hardcode mật khẩu, lộ API key, thuật toán mã hóa yếu).
* **Hành động:** SonarQube sẽ chỉ rõ dòng code bị nghi ngờ và đề xuất cách sửa (ví dụ: khuyên dùng Bcrypt thay cho MD5, sử dụng biến môi trường thay vì ghi cứng chuỗi bí mật).

### 3. Code Smells (Mã xấu/Rác code)
* **Ý nghĩa:** Code vẫn chạy đúng nhưng viết không tối ưu, khó bảo trì, vi phạm quy tắc lập trình (ví dụ: đặt tên sai convention, phương thức quá dài, import thừa, biến khai báo nhưng không dùng).
* **Hành động:** Tiến hành Refactor mã nguồn trong các đợt cải tiến (Sprint Retrospective).

### 4. Coverage (Độ bao phủ kiểm thử)
* **Ý nghĩa:** Tỷ lệ mã nguồn được bảo vệ bởi các bài viết Unit Test. Chỉ số này được SonarQube đồng bộ trực tiếp từ báo cáo JaCoCo (`target/site/jacoco/index.html`).
* **Mục tiêu dự án:** Cần đảm bảo độ bao phủ của các package dịch vụ chính (`service.impl`) đạt trên **50%**.

### 5. Duplications (Lặp code)
* **Ý nghĩa:** Tỷ lệ phần trăm các dòng code bị sao chép (Copy-Paste) ở nhiều nơi trong dự án thay vì viết thành hàm dùng chung (vi phạm nguyên tắc DRY - Don't Repeat Yourself).
* **Mục tiêu:** Tỷ lệ lặp code lý tưởng phải dưới **3%**.

---

## 🛠️ 4. Sửa lỗi dựa trên gợi ý của SonarQube
Nhấp trực tiếp vào từng lỗi trong danh sách **Issues** trên giao diện SonarQube, công cụ sẽ hiển thị:
* Dòng code lỗi.
* Nguyên nhân tại sao dòng này được coi là lỗi.
* Tab **"How can I fix it?"** đưa ra code ví dụ trước và sau khi sửa (đây là tài liệu vô cùng quý giá cho các lập trình viên).
