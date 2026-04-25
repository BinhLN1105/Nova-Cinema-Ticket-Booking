# Database Migration Skill

Kỹ năng này định nghĩa cách thức quản lý và cập nhật cấu trúc cơ sở dữ liệu (Schema Evolution) trong dự án NovaTicket.

## 1. Mẫu Thiết Kế Hiện Tại (Current Patterns)

### 1.1 Khởi tạo schema (Flyway)
Hệ thống đã triển khai **Flyway** để quản lý cấu trúc cơ sở dữ liệu tự động.
- **Thư mục cấu trúc**: `src/main/resources/db/migration/`.
- **Script nền tảng**: `V1__Initial_Schema.sql` (chứa toàn bộ cấu trúc bảng từ Entity).
- **Dữ liệu hạt giống**: `V2__Insert_Seed_Data.sql` (chứa dữ liệu mẫu cho Combo, v.v.).

### 1.2 Database Tech Stack
- **Engine**: PostgreSQL (pgvector extension enabled).
- **Migration**: Flyway (Baseline version: 0).
- **DDL-Auto**: `spring.jpa.hibernate.ddl-auto=none` (Flyway làm chủ schema).

## 2. Nợ Kỹ Thuật (Technical Debt)
- [ ] **Data Integrity**: Cần kiểm tra lại các ràng buộc khóa ngoại (Foreign Keys) trong `V1` so với thực tế DB cũ để đảm bảo không có sai lệch dữ liệu.
- [ ] **Baseline Training**: Cần hướng dẫn team chạy lệnh `mvn flyway:baseline` trên các máy dev cũ đã có sẵn database.

## 3. Quy Tắc Cho Code Mới (Rules for New Code)

### 3.1 Bắt buộc dùng Flyway
Mọi thay đổi cấu trúc database từ nay về sau (Create, Alter, Drop table/column) **PHẢI** được thực hiện thông qua Flyway Migration Scripts.

### 3.2 File Naming Convention
Các file script phải đặt trong thư mục `src/main/resources/db/migration/` với định dạng:
- `V[VERSION]__[DESCRIPTION].sql`
- Ví dụ: `V1__initial_schema.sql`, `V2__add_movie_backdrop_url.sql`.
- Sử dụng dấu gạch dưới kép (`__`) để phân cách version và mô tả.

### 3.3 Quy tắc viết SQL
- **Idempotency**: Scripts nên được viết sao cho an toàn (ví dụ: `CREATE TABLE IF NOT EXISTS`).
- **Data Types**: Sử dụng `UUID` cho Primary Key của các bảng dữ liệu thay vì `Long` (phù hợp với cấu trúc hiện tại của dự án).
- **Constraints**: Luôn khai báo đầy đủ `NOT NULL`, `DEFAULT`, `FOREIGN KEY` trong script thay vì để Hibernate tự sinh.
- **Reversible**: Mỗi script nên tập trung vào một thay đổi nhỏ để dễ dàng rollback khi cần.
