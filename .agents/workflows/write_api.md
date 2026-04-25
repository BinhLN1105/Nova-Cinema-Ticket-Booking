---
description: Viết API endpoint mới cho NovaTicket Spring Boot backend — hỏi thông tin, sinh đầy đủ DTO + Controller + Service + Repository + Unit Test + Swagger doc theo chuẩn dự án.
---

## Steps

### 1. Thu thập yêu cầu
Hỏi người dùng các thông tin sau (nếu chưa có):
- **Tên endpoint**: HTTP method + path (VD: `POST /api/v1/bookings`)
- **Mô tả**: Endpoint này làm gì? (1-2 câu)
- **Input**: Những field nào cần nhận? Validation rule?
- **Output**: Trả về gì khi thành công?
- **Ai được gọi**: PUBLIC (không cần auth) / USER / ADMIN?
- **Module**: thuộc domain nào? (`booking` / `payment` / `movie` / `auth` / `loyalty`)

### 2. Thiết kế Contract (trình bày để confirm trước khi code)
Trình bày rõ ràng:
```
METHOD  PATH
Request Body: { field: type, validation }
Response:     { field: type }
HTTP Codes:   200 / 201 / 400 / 401 / 403 / 404 / 409
Auth:         PUBLIC / @PreAuthorize("hasRole('USER')")
```
**Chờ người dùng confirm trước khi sang bước 3.**

### 3. Tạo Request DTO
```java
// Package: com.nova.{module}.dto.request
// File: {Action}{Entity}Request.java
public class CreateBookingRequest {
    @NotNull(message = "...")
    private Long scheduleId;
    // ... các field khác với annotation validation
}
```
Quy tắc:
- Dùng `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`
- Message lỗi phải rõ ràng bằng tiếng Anh
- Dùng `BigDecimal` cho tiền, `LocalDateTime` cho thời gian
- Không dùng `double` hoặc `float` cho tiền

### 4. Tạo Response DTO
```java
// Package: com.nova.{module}.dto.response
// File: {Entity}Response.java
public class BookingResponse {
    private Long id;
    private String bookingCode;
    // ... chỉ expose field cần thiết, KHÔNG expose password/internal data
}
```

### 5. Tạo Controller (THIN — không chứa logic)
```java
@RestController
@RequestMapping("/api/v1/{path}")
@RequiredArgsConstructor
@Validated
@Tag(name = "{Module} API", description = "...")  // Swagger
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    @PostMapping
    @Operation(summary = "...", description = "...")  // Swagger
    @PreAuthorize("hasRole('USER')")  // nếu cần auth
    public ResponseEntity<ApiResponse<{Entity}Response>> create{Entity}(
            @RequestBody @Valid Create{Entity}Request request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        {Entity}Response response = {entity}Service.create{Entity}(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "{Entity} created successfully"));
    }
}
```

### 6. Tạo Service (BUSINESS LOGIC tại đây)
```java
@Service
@RequiredArgsConstructor
@Transactional  // cho write operations
@Slf4j
public class {Entity}ServiceImpl implements {Entity}Service {

    private final {Entity}Repository {entity}Repository;

    @Override
    public {Entity}Response create{Entity}(Create{Entity}Request request, Long userId) {
        log.debug("Creating {entity} for user: {}", userId);
        // 1. Validate business rules
        // 2. Map request → Entity
        // 3. Save
        // 4. Map Entity → Response
        // 5. Return
    }
}
```
Quy tắc:
- `@Transactional` cho mọi write operation
- Throw `{Entity}NotFoundException` (custom) thay vì `RuntimeException`
- Log ở mức `DEBUG` cho input/output, `ERROR` cho exception

### 7. Tạo Repository (nếu chưa có)
```java
@Repository
public interface {Entity}Repository extends JpaRepository<{Entity}, Long> {
    // Dùng JPQL nếu cần query phức tạp, KHÔNG dùng raw SQL
    @Query("SELECT e FROM {Entity} e WHERE e.userId = :userId AND e.status = :status")
    List<{Entity}> findByUserIdAndStatus(@Param("userId") Long userId,
                                          @Param("status") {Entity}Status status);
}
```

### 8. Viết Unit Test cho Service layer
```java
@ExtendWith(MockitoExtension.class)
class {Entity}ServiceTest {

    @Mock private {Entity}Repository {entity}Repository;
    @InjectMocks private {Entity}ServiceImpl {entity}Service;

    @Test
    @DisplayName("Should create {entity} successfully when valid input")
    void create{Entity}_ValidInput_ReturnsResponse() { ... }

    @Test
    @DisplayName("Should throw exception when {entity} not found")
    void create{Entity}_InvalidId_ThrowsException() { ... }
}
```
Tạo tối thiểu: 1 happy path + 1 error case cho mỗi method.

### 9. Kiểm tra Swagger tự động
- Xác nhận `@Tag`, `@Operation` đã được thêm vào Controller
- Nhắc người dùng: chạy app và kiểm tra tại `http://localhost:8080/swagger-ui.html`

### 10. Tóm tắt những gì đã tạo
In ra danh sách file đã tạo/sửa:
```
✅ Created: src/.../dto/request/Create{Entity}Request.java
✅ Created: src/.../dto/response/{Entity}Response.java
✅ Created: src/.../controller/{Entity}Controller.java
✅ Created: src/.../service/{Entity}Service.java (interface)
✅ Created: src/.../service/impl/{Entity}ServiceImpl.java
✅ Created: src/.../repository/{Entity}Repository.java
✅ Created: src/test/.../service/{Entity}ServiceTest.java
```
