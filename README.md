# 🎬 Smart Cinema Booking System (Backend API)

Hệ thống quản lý và đặt vé xem phim toàn diện, hỗ trợ đa nền tảng (Mobile App & Web Admin). Backend được xây dựng theo kiến trúc RESTful API vững chắc, tối ưu hóa cho môi trường Cloud và tích hợp AI thông minh.

## 🚀 Tính Năng Nổi Bật (Key Features)

* **🎭 Quản lý Phim & Lịch Chiếu:** Quản lý danh mục phim, rạp chiếu, phòng chiếu (2D, 3D, IMAX) và suất chiếu động.
* **💺 Đặt Vé Thời Gian Thực (Real-time Booking):** Xử lý triệt để bài toán **Race Condition** (nhiều người cùng đặt 1 ghế) bằng cơ chế `Optimistic Locking` (@Version).
* **🎫 Vé Điện Tử (E-Ticket):** Sinh QR Code duy nhất cho mỗi đơn hàng (Booking), hỗ trợ check-in quét mã tại rạp mượt mà.
* **🎁 Khuyến Mãi & Thanh Toán:** Quản lý Voucher (giảm theo % hoặc tiền cố định), tính toán giỏ hàng (bắp nước) và tích hợp thanh toán VNPay.
* **🔔 Thông Báo Đẩy (Push Notifications):** Tích hợp **Firebase Cloud Messaging (FCM)** gửi thông báo tự động (Nhắc lịch xem phim, vé thành công) đến Mobile App.
* **🧠 Trợ Lý Ảo Nova (AI Chatbot):** Tích hợp chatbot thông minh hỗ trợ tra cứu lịch chiếu, giá vé, chính sách và tư vấn phim. Sử dụng kiến trúc **RAG (Retrieval-Augmented Generation)** với Python FastAPI, ChromaDB (Vector Store) và Google Gemini AI. Hỗ trợ phản hồi giàu định dạng (Markdown) và chế độ **Safe Mode** khi vượt quota.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

### Backend (Main Server)
* **Ngôn ngữ:** Java 21
* **Framework:** Spring Boot 3.2.5 (Spring Web, Spring Data JPA, Spring Security, Validation)
* **Build Tool:** Maven
* **Tiện ích:** Lombok, Spring Boot DevTools

### AI & RAG Service (Python)
* **Ngôn ngữ:** Python 3.12+
* **Framework:** FastAPI, LangChain
* **LLM:** Google Gemini 
* **Vector DB:** ChromaDB (Local Persistence)
* **Embedding:** sentence-transformers (Local)

### Database & Cloud Services
* **Database:** Supabase (PostgreSQL) trên Cloud
* **Push Notification:** Firebase Admin SDK
* **Lưu trữ Ảnh/Video:** Cloudinary / Supabase Storage (Dự kiến)

---

## ⚙️ Hướng Dẫn Cài Đặt (Local Setup)

### 1. Yêu cầu hệ thống
* JDK 21 cài đặt sẵn trên máy.
* Maven (hoặc sử dụng wrapper `mvnw` đi kèm).
* Tài khoản **Supabase** (đã tạo project).
* Tài khoản **Firebase** (để lấy file `service-account-key.json`).

### 2. Cấu hình Database (Quan trọng)
Hệ thống sử dụng cơ sở dữ liệu vector cho tính năng AI. Trước khi chạy code, bạn **phải** thực thi lệnh sau trên SQL Editor của Supabase để kích hoạt tính năng:

```sql
CREATE EXTENSION IF NOT EXISTS vector;