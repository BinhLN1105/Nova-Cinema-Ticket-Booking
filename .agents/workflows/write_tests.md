---
description: Viết test cho NovaTicket — AI tự quyết định loại test phù hợp (Unit hoặc Integration) dựa trên target được chỉ định, sau đó chạy và báo kết quả.
---

## Steps

### 1. Xác định target cần test
Hỏi người dùng nếu chưa rõ:
- Test cái gì? (tên class, method, hoặc feature)
- Có test case cụ thể nào muốn cover không?
- Có bug cụ thể nào vừa fix cần regression test không?

### 2. AI tự quyết định loại test phù hợp

**→ Viết Unit Test khi:**
- Target là Service layer (business logic)
- Target là một utility/helper class
- Cần test nhiều edge case nhanh
- Không cần DB thực hoặc HTTP thực

**→ Viết Integration Test khi:**
- Target là Controller (cần test cả Security filter, validation, response format)
- Target là Repository với query phức tạp (cần DB thực)
- Cần test luồng end-to-end của một feature

**→ Viết cả hai khi:**
- Feature phức tạp (VD: booking flow, payment flow)
- AI sẽ tạo Unit Test cho Service + Integration Test cho Controller

### 3A. Nếu Unit Test — dùng Mockito
```java
@ExtendWith(MockitoExtension.class)
class {ClassName}Test {

    @Mock
    private {Dependency}Repository {dependency}Repository;

    @InjectMocks
    private {ClassName}Impl {className};

    // Test naming: methodName_Condition_ExpectedBehavior
    @Test
    @DisplayName("Should return booking when valid scheduleId and seats provided")
    void createBooking_ValidInput_ReturnsBookingResponse() {
        // Arrange
        var request = buildValidRequest();
        var mockEntity = buildMockEntity();
        when({dependency}Repository.findById(anyLong())).thenReturn(Optional.of(mockEntity));

        // Act
        var result = {className}.createBooking(request, USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBookingCode()).isNotBlank();
        verify({dependency}Repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when schedule not found")
    void createBooking_InvalidScheduleId_ThrowsNotFoundException() {
        // Arrange
        when(scheduleRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> {className}.createBooking(request, USER_ID))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessageContaining("Schedule not found");
    }
}
```

### 3B. Nếu Integration Test — dùng @SpringBootTest
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // rollback sau mỗi test
class {Controller}IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/{path} - should return 201 when valid request")
    void create{Entity}_ValidRequest_Returns201() throws Exception {
        var request = buildValidRequest();

        mockMvc.perform(post("/api/v1/{path}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /api/v1/{path} - should return 401 when no auth")
    void create{Entity}_NoAuth_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/{path}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
```

### 4. Test case bắt buộc phải có
Với mỗi method/endpoint, tạo tối thiểu:

| # | Loại | Mô tả |
|---|---|---|
| 1 | ✅ Happy path | Input hợp lệ → kết quả đúng |
| 2 | ❌ Not found | Entity không tồn tại → exception |
| 3 | 🔒 Auth | Không có token / sai role → 401/403 |
| 4 | ⚠️ Validation | Input thiếu field bắt buộc → 400 |

Thêm case đặc thù nếu có:
- **Booking**: test seat đã bị lock bởi người khác
- **Payment**: test VNPay callback gọi 2 lần (idempotency)
- **Loyalty**: test không bị double-credit điểm

### 5. Chạy test và báo kết quả
```bash
# Unit test cho module cụ thể
// turbo
mvn test -pl backend -Dtest={ClassName}Test

# Tất cả test
// turbo
mvn test -pl backend
```

Sau khi chạy, báo cáo:
```
✅ Tests passed: X
❌ Tests failed: Y
  └── {TestName}: {lý do fail ngắn gọn}
⚠️  Tests skipped: Z
```

Nếu có test fail → phân tích nguyên nhân và đề xuất fix ngay.

### 6. Tóm tắt file đã tạo
```
✅ Created: src/test/.../service/{ClassName}Test.java        (Unit)
✅ Created: src/test/.../controller/{ClassName}IT.java       (Integration)
```
