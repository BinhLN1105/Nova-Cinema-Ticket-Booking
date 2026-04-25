# API Design Skill

Kỹ năng này định nghĩa các quy tắc thiết kế và triển khai REST API trong hệ thống NovaTicket, đảm bảo tính nhất quán giữa Backend (Spring Boot), AI Service (FastAPI) và các Client (Android, React).

## 1. Mẫu Thiết Kế Hiện Tại (Current Patterns)

### 1.1 Cấu trúc phản hồi (Response Wrapper)
Tất cả các API thành công phải được bao bọc trong đối tượng `ApiResponse<T>`.

**Mẫu JSON thành công:**
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { ... }
}
```

**Mẫu JSON lỗi:**
Dùng `ErrorResponse` thông qua `GlobalExceptionHandler`.
```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "email": "Email không đúng định dạng"
  }
}
```

### 1.2 Phân lớp Controller
- **Annotation**: Sử dụng `@RestController` và `@RequestMapping("/api/v1/...")`.
- **Dependency**: Inject Service thông qua Constructor (`@RequiredArgsConstructor`).
- **Validation**: Sử dụng `@Valid` cho các Request Body DTO.
- **Security**: Sử dụng `@PreAuthorize` để kiểm soát quyền truy cập ở cấp độ method.

## 2. Nợ Kỹ Thuật (Technical Debt)
- [ ] **Thống nhất cấu trúc lỗi**: Hiện tại AI Service (FastAPI) và Backend (Spring Boot) có cấu trúc `ErrorResponse` chưa hoàn toàn đồng nhất (FastAPI dùng mặc định của Pydantic/FastAPI).
- [ ] **Versioning**: Mặc dù đã có `/v1/` trong URL, nhưng chưa có chiến lược versioning rõ ràng khi có thay đổi lớn (Breaking changes).
- [ ] **Documentation**: Swagger/OpenAPI (`/swagger-ui.html`) đã có nhưng các mô tả `@Operation` còn sơ sài.

## 3. Quy Tắc Cho Code Mới (Rules for New Code)

### 3.1 Endpoint Naming
- Sử dụng **kebab-case** cho đường dẫn (ví dụ: `/movie-showtimes`).
- Sử dụng danh từ số nhiều cho resources (ví dụ: `/movies`, `/cinemas`).
- Hành động đặc biệt (không phải CRUD) dùng ở cuối URL (ví dụ: `/movies/{id}/cancel`).

### 3.2 HTTP Methods & Status Codes
- `GET`: Lấy dữ liệu (200 OK).
- `POST`: Tạo mới (201 Created).
- `PUT`: Cập nhật toàn bộ (200 OK).
- `PATCH`: Cập nhật một phần (200 OK).
- `DELETE`: Xóa (200 OK hoặc 204 No Content).

### 3.3 Data Transfer Objects (DTO)
- **KHÔNG** trả về Entity trực tiếp từ Controller.
- Sử dụng `MapStruct` để chuyển đổi giữa Entity và DTO.
- Tách biệt `RequestDTO` (Input) và `ResponseDTO` (Output).

### 3.4 Validation
- Luôn sử dụng `jakarta.validation` (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, v.v.) trong các Request DTO.
- Các logic validate phức tạp (liên quan đến DB) phải đặt ở Service Layer.
