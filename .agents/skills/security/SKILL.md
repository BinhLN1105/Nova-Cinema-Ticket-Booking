# Security Skill

Kỹ năng này định nghĩa các tiêu chuẩn bảo mật cho dự án NovaTicket, tập trung vào xác thực (Authentication), phân quyền (Authorization) và bảo vệ dữ liệu.

## 1. Mẫu Thiết Kế Hiện Tại (Current Patterns)

### 1.1 Stateless Authentication (JWT)
Hệ thống sử dụng **JSON Web Token (JWT)** để quản lý phiên làm việc của người dùng.
- **Library**: `jjwt` version 0.12.5.
- **Filter**: `JwtAuthenticationFilter` trích xuất Token từ header `Authorization: Bearer <token>`.
- **Session**: `SessionCreationPolicy.STATELESS` (không lưu session trên server).

### 1.2 Access Control
- **Spring Security**: Sử dụng `SecurityFilterChain` để cấu hình public/private endpoints.
- **Method Security**: Sử dụng `@PreAuthorize("hasRole('ADMIN')")` hoặc `hasAnyRole(...)` trực tiếp trên các method của Controller.
- **Password Hashing**: Sử dụng `BCryptPasswordEncoder`.

### 1.3 Internal Security
Giao tiếp giữa Backend (Java) và AI Service (FastAPI) được bảo vệ bằng **Internal API Key**.
- Header: `X-Internal-Key`.
- Check logic: Thực hiện trong Controller hoặc Middleware (`verify_internal_key` trong FastAPI).

## 2. Nợ Kỹ Thuật (Technical Debt)
- [ ] **Broad CORS**: Hiện tại `setAllowedOriginPatterns(List.of("*"))` trong `SecurityConfig.java` đang quá rộng, cần giới hạn lại danh sách domain cụ thể khi lên môi trường Production.
- [ ] **Token Revocation**: Chưa có cơ chế Blacklist Token khi người dùng Logout (Token vẫn còn hiệu lực đến khi hết hạn tự nhiên).
- [ ] **Secret Management**: Các JWT Secret, API Keys đang được lấy từ biến môi trường nhưng cần đảm bảo quy trình quản lý secret an toàn trong CI/CD.

## 3. Quy Tắc Cho Code Mới (Rules for New Code)

### 3.1 Authorization First
Mọi API mới **PHẢI** được cấu hình quyền truy cập rõ ràng trong `SecurityConfig` hoặc sử dụng `@PreAuthorize` nếu cần kiểm soát sâu.

### 3.2 Secure Coding
- Tuyệt đối **KHÔNG** log Token hoặc Password của người dùng.
- Sử dụng `@AuthenticationPrincipal` để lấy thông tin `User` hiện tại một cách an toàn thay vì query lại từ DB nhiều lần.
- Luôn validate quyền sở hữu (Ownership) khi người dùng cập nhật/xóa tài nguyên (ví dụ: Chỉnh sửa review của chính mình).

### 3.3 Sensitive Data
- Dữ liệu sensitive như số tiền, số điện thoại phải được validate chặt chẽ ở server side, không tin tưởng hoàn toàn vào client side validation.
