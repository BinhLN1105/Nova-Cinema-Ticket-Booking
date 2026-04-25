---
description: Debug lỗi trong dự án NovaTicket — AI tự phân tích loại lỗi và chọn hướng debug phù hợp, từ đọc log đến trace root cause.
---

## Steps

### 1. Thu thập thông tin lỗi
Hỏi người dùng (nếu chưa cung cấp):
- Lỗi xảy ra ở layer nào? (Backend / Frontend / Mobile / AI Service)
- Có stack trace không? Paste vào đây.
- Lỗi xảy ra lúc nào? (Luôn luôn / Thỉnh thoảng / Sau khi deploy / Sau khi thay đổi X)
- Môi trường: Local / Staging / Production?

### 2. Phân loại lỗi & chọn hướng debug
AI tự phân tích stack trace / mô tả lỗi để xác định loại:

**🔴 Lỗi Runtime Exception (NullPointer, ClassCast...)**
→ Đọc stack trace → xác định dòng lỗi → trace ngược lên caller → tìm data null/sai type

**🟡 Lỗi Business Logic (sai kết quả, không đúng nghiệp vụ)**
→ Check DB state → reproduce với data thực → trace qua Service layer → tìm điều kiện sai

**🔵 Lỗi API / HTTP (4xx, 5xx, CORS, timeout)**
→ Check request/response log → check Security filter chain → check config → check DB connection

**🟠 Lỗi VNPay / Payment**
→ Check VNPay callback log trước → verify checksum → check transaction status trong DB → check idempotency

**🟣 Lỗi Auth / JWT**
→ Check token expiry → check Security config → check role assignment trong DB → check filter order

**⚫ Lỗi Android / Mobile**
→ Check Logcat → check API response thực tế → check network config (BASE_URL) → check local.properties

### 3. Đọc Log có cấu trúc
- Tìm file log hoặc yêu cầu paste log liên quan
- Tìm timestamp của lỗi đầu tiên (không phải lỗi cascade)
- Tìm request ID / correlation ID nếu có
- Bỏ qua các WARNING không liên quan, tập trung vào ERROR đầu tiên

### 4. Trace root cause
- Xác định chính xác file + dòng code gây lỗi
- Đọc code xung quanh để hiểu context
- Kiểm tra data đầu vào tại điểm lỗi
- Nếu liên quan DB: kiểm tra query thực tế Hibernate sinh ra (enable SQL log nếu cần)

### 5. Đề xuất fix
- Đưa ra **1 fix chính** (root cause) trước
- Đưa ra fix bổ sung nếu có (defensive coding, validation thêm)
- Giải thích tại sao lỗi xảy ra bằng tiếng Việt ngắn gọn
- Nếu là nợ kỹ thuật đã biết → ghi chú rõ, không tự ý refactor rộng

### 6. Verify sau khi fix
- Đề xuất cách test lại: unit test cụ thể / curl command / test case thủ công
- Kiểm tra fix không gây regression ở các module liên quan
- Nếu fix ảnh hưởng VNPay hoặc Auth → nhắc test kỹ hơn bình thường

### Lưu ý đặc biệt NovaTicket
- Lỗi liên quan **seat locking** → kiểm tra race condition, check transaction isolation
- Lỗi liên quan **VNPay callback** → kiểm tra idempotency (callback có thể gọi nhiều lần)
- Lỗi liên quan **loyalty points** → kiểm tra transaction, tránh double-credit
- Lỗi liên quan **RAG chatbot** → check FastAPI service có đang chạy không (port 8000), check vector DB connection
