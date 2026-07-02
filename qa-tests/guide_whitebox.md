# Hướng dẫn kiểm thử hộp trắng (Whitebox Testing) với JUnit 5, Mockito và JaCoCo

Tài liệu này hướng dẫn các thành viên cách viết, tổ chức Unit Test cho tầng nghiệp vụ (Service Layer) của dự án **NovaTicket** bằng **JUnit 5**, cách mock dữ liệu bằng **Mockito**, và đo lường độ bao phủ mã nguồn bằng **JaCoCo**.

---

## 📂 1. Cấu trúc và Thiết lập một lớp Unit Test

Trong kiểm thử hộp trắng tầng Service, chúng ta **không** khởi động toàn bộ Spring Boot Server (tránh làm chậm thời gian test và tránh ghi dữ liệu thật vào Database). Thay vào đó, ta sử dụng **MockitoExtension** để cô lập hoàn toàn Service cần test và mock (giả lập) tất cả các Repositories/Services phụ thuộc.

### Khai báo một lớp Test chuẩn (Ví dụ mẫu):
```java
package com.cinema.ticket_booking.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.model.Booking;

@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito trong JUnit 5
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository; // Giả lập Repository (không kết nối DB thật)

    @InjectMocks
    private BookingServiceImpl bookingService; // Tự động tiêm các mock trên vào Service thực tế cần test

    // Các phương thức test viết tại đây...
}
```

---

## 🚀 2. Kỹ thuật viết kịch bản Test (Mocking & Assertions)

Mỗi phương thức kiểm thử hộp trắng cần tuân thủ cấu trúc **AAA (Arrange - Act - Assert)**:

### 1. Arrange (Thiết lập giả lập)
Sử dụng các hàm của Mockito để định nghĩa trước hành vi cho các mock object khi chúng được gọi:
```java
// Giả lập: khi repository tìm kiếm booking theo ID "booking-123", trả về một thực thể Booking giả lập
Booking mockBooking = new Booking();
mockBooking.setId("booking-123");
mockBooking.setStatus(BookingStatus.PENDING);

when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(mockBooking));
```

### 2. Act (Kích hoạt phương thức thực tế)
Gọi phương thức nghiệp vụ thực tế của Service cần kiểm thử:
```java
bookingService.cancelBooking("booking-123", false);
```

### 3. Assert (Xác thực kết quả)
Sử dụng các assertions của JUnit 5 để so sánh giá trị trả về thực tế với giá trị kỳ vọng, hoặc bắt ngoại lệ:

*   **Xác thực giá trị thuộc tính:**
    ```java
    assertEquals(BookingStatus.CANCELLED, mockBooking.getStatus());
    ```
*   **Xác thực ném ngoại lệ (Exception):**
    ```java
    assertThrows(BadRequestException.class, () -> {
        bookingService.cancelBooking("booking-expired", false);
    });
    ```
*   **Verify (Xác thực số lần gọi hàm của Mock):**
    Đảm bảo hàm lưu trữ (save) hoặc hàm gửi mail được kích hoạt đúng số lần trong luồng xử lý:
    ```java
    verify(bookingRepository, times(1)).save(mockBooking);
    ```

---

## 📊 3. Chạy kiểm thử và đo lường Coverage bằng JaCoCo

### Cách chạy test và xuất báo cáo
Từ thư mục root của Backend (`Backend/ticket-booking`), chạy lệnh:
```bash
mvn clean test
```
Lệnh này sẽ chạy toàn bộ unit tests và tự động kích hoạt **JaCoCo agent** ghi nhận dữ liệu thực thi. Báo cáo HTML sẽ được xuất ra tại thư mục:
`Backend/ticket-booking/target/site/jacoco/index.html`

### Cách đọc báo cáo JaCoCo trong mã nguồn:
Khi mở báo cáo hoặc xem trong IDE tích hợp, các dòng code sẽ được đánh dấu bằng 3 màu:
1.  **Màu xanh lá (Green):** Dòng code đã được chạy qua hoàn chỉnh trong bài test (cả nhánh Đúng và Sai nếu là câu điều kiện).
2.  **Màu đỏ (Red):** Dòng code chưa từng được chạy qua bất kỳ bài test nào. Lập trình viên cần viết bổ sung test case để đi qua dòng này.
3.  **Màu vàng (Yellow):** Dòng code là câu điều kiện rẽ nhánh nhưng chỉ mới được kiểm thử một nửa (ví dụ: chỉ mới test trường hợp `if` thỏa mãn, chưa test trường hợp `if` không thỏa mãn).

### Tiêu chí nghiệm thu chất lượng (Quality Gate):
*   **Instruction Coverage** (Bao phủ câu lệnh) của toàn bộ `service.impl` phải đạt tối thiểu **50%**.
*   **Branch Coverage** (Bao phủ nhánh rẽ) phải đạt tối thiểu **40%**.
