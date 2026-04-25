<div align="center">
  <img src="https://img.icons8.com/color/96/000000/movie-projector.png" width="80" alt="Cinema Projector Icon"/>
  <h1>🎬 NovaTicket — Smart Cinema Booking System</h1>
  <p><b>Hệ thống quản lý và đặt vé rạp chiếu phim toàn diện với mô hình Monorepo</b></p>
  <p><i>(Mobile App, Web CMS, Spring Boot Backend)</i></p>

  <div>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React" />
    <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
    <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  </div>
</div>

<br/>

## 📖 Tham Khảo Nhanh (Table of Contents)
- [🌟 Giới Thiệu Dự Án](#-giới-thiệu-dự-án)
- [💻 Công Nghệ Sử Dụng (Tech Stack)](#-công-nghệ-sử-dụng)
- [📸 Giao Diện Nổi Bật (Showcase)](#-giao-diện-nổi-bật)
- [📂 Cấu Trúc Thư Mục (Folder Structure)](#-cấu-trúc-thư-mục-monorepo)
- [🧩 Các Thành Phần & Tính Năng](#-các-thành-phần--tính-năng)
- [🚀 Hướng Dẫn Khởi Chạy (Getting Started)](#-hướng-dẫn-khởi-chạy-getting-started)
- [🛠 Lưu Ý Cấu Hình](#-lưu-ý-cấu-hình)

---

## 🌟 Giới Thiệu Dự Án
**NovaTicket** là một dự án ứng dụng rạp chiếu phim đa nền tảng. Hệ thống được thiết kế linh hoạt để phục vụ toàn diện vòng đời của một rạp chiếu phim: từ việc khách hàng mua vé trên **Ứng dụng di động**, đến việc nhân viên bán vé trực tiếp tại quầy qua **Web POS**, và được điều phối bởi một hệ thống **Backend RESTful API** vững chắc.

Dự án tích hợp sâu các tiện ích thanh toán trực tuyến (`VNPay`), hệ thống ví nội bộ (`CinePoint`), thông báo đẩy (`FCM`), và sử dụng `PostgreSQL` với `pgvector` cho các tính năng tìm kiếm thông minh.

---

## 💻 Công Nghệ Sử Dụng

- **📱 Mobile App**: Java (Android Native), MVVM, Hilt, Navigation Component, Retrofit 2.
- **🗚️ Web Frontend**: React.js, Zustand (State Management), Tailwind CSS, Vite.
- **⚙️ Backend Core**: Java 21, Spring Boot 4, Spring Security (JWT, Google OAuth2), Hibernate / JPA, Flyway.
- **🤖 AI Service**: Python 3.10+, FastAPI, LangChain, **FAISS** (Vector DB), **Cohere API** (Embedding), **Google Gemini API** (LLM).
- **🗄️ Database & Cache**: PostgreSQL (Supabase), Redis (Token Blacklisting & Caching).
- **🔌 3rd Party Integration**: VNPay API, Firebase Cloud Messaging (FCM), Cloudinary (Image Storage).

---

## 📸 Giao Diện Nổi Bật

<div align="center">
  <img src="https://via.placeholder.com/250x500.png?text=Mobile+App" width="220" alt="Mobile App Showcase" style="border-radius: 12px; margin-right: 15px;" />
  <img src="https://via.placeholder.com/500x500.png?text=Web+POS+Dashboard" width="450" alt="Web POS Dashboard" style="border-radius: 12px;" />
</div>

---

## 📂 Cấu Trúc Thư Mục (Monorepo)

```text
.
├── App/                            # Ứng dụng Mobile Android
├── Backend/
│   ├── ticket-booking/             # Project Spring Boot API
│   └── ai-booking/                 # Python AI RAG Service (FastAPI)
├── Frontend/                       # Web Frontend (Admin/Staff CMS & POS System)
├── Database/                       # Scripts SQL cấu trúc
├── docs/                           # Tài liệu thiết kế hệ thống
└── Cinema_Entity_Design.docx       # Tài liệu thiết kế Database (ERD)
```

---

## 🧩 Các Thành Phần & Tính Năng

### 1. 📱 App (Mobile - Dành cho Khách Hàng)
- **Đặt vé tiện lợi:** Xem lịch chiếu, đặt vé (chọn ghế, mua kèm bắp nước).
- **Thanh toán đa kênh:** Hỗ trợ thanh toán qua mã VNPay hoặc thanh toán bằng Điểm thưởng nội bộ (`CinePoint`).
- **Thông báo & E-Ticket:** Quản lý thông báo cá nhân hóa (FCM Data Messages) & Xem lại vé QR Code để check-in tại quầy.
- **Đánh giá phim:** Hệ thống review yêu cầu "Verified Purchase" (đã thanh toán thành công) mới được viết bài.

### 2. 🖥️ Frontend (Web CMS & Hệ thống POS)
- **Hệ thống POS (Quầy vé):** Giao diện bán vé và bắp nước tốc độ cao dành cho nhân viên, hỗ trợ thanh toán tiền mặt và quét mã.
- **Admin Dashboard:** Cấu hình cụm rạp, phòng chiếu, trang chủ quản lý phim, lên lịch suất chiếu chi tiết.
- **Báo cáo doanh thu:** Biểu đồ trực quan và báo cáo doanh thu vé, doanh thu bắp nước dựa trên bảng biểu thời gian thực.

### 3. ⚙️ Bộ Tính Năng Backend Cốt Lõi
Hệ thống xử lý nghiệp vụ phức tạp với **13 phân hệ chức năng**:
1. **Xác thực:** Đăng nhập bằng Email / Google OAuth2, xử lý Access/Refresh Token.
2. **Quản lý Phim & Rạp:** Lập danh mục phim, bản đồ phòng chiếu linh hoạt.
3. **Lịch chiếu & Ghế ngồi:** Cơ chế Lock ghế Real-time, tính năng `Override Seat Prices` để thiết lập lại giá ghế theo khung giờ.
4. **Quy tắc giá động (Dynamic Pricing):** Tự động đẩy giá vé theo Ngày/Giờ Vàng, loại vé (VIP/Sweetbox).
5. **Ví CinePoint:** Hệ thống xếp hạng thành viên (Bronze, Silver, Gold, Diamond), tiêu xu mua vé.
6. **Thẻ quà tặng / Khuyến mãi:** Nạp ví E-Card, sinh mã voucher giảm giá vào giỏ hàng.
7. **Thông báo (FCM Push):** Bắn thông báo theo Device Token.

---

## 🚀 Hướng Dẫn Khởi Chạy (Getting Started)

### 📋 Yêu Cầu Hệ Thống (Prerequisites)
- **JDK 17/21+**
- **Node.js 18+** & npm/yarn
- **Python 3.10+** (Yêu cầu cho AI RAG Service)
- **PostgreSQL** (Yêu cầu phải cài đặt extension `pgvector`)
- **Redis Server** (Cổng `6379`)
- **Android Studio** (Dùng để build phiên bản Mobile APP)

### 1️⃣ Khởi chạy Backend (Spring Boot)
Di chuyển vào thư mục `Backend/ticket-booking` và thiết lập file `.env` (hoặc cấu hình trong `application-dev.yml`) cho DB, Redis, VNPay:

```bash
cd Backend/ticket-booking
# Cài đặt maven dependencies và khởi chạy server
mvn clean spring-boot:run
```
> 💡 Server Backend sẽ lắng nghe tại: `http://localhost:8080`

### 2️⃣ Khởi chạy Frontend (React Web)
Vào thư mục `Frontend` và cung cấp các giá trị môi trường như API URL tại `.env`:

```bash
cd Frontend
npm install
npm run dev
```
> 💡 Trang Web bán vé / CMS sẽ phân phối tại: `http://localhost:5173`

### 3️⃣ Khởi chạy Mobile App (Android)
1. Mở thư mục `App/` bằng trình duyệt **Android Studio**.
2. Đợi Sync Gradle. Cấu hình file `local.properties` (ví dụ: `BASE_URL=http://localhost:8080/api/`).
3. Chạy App trên Emulator hoặc thiết bị thật.

### 4️⃣ Khởi chạy AI RAG Service (Python)
Dịch vụ AI chịu trách nhiệm trả lời câu hỏi người dùng dựa trên ngữ cảnh (RAG) và tích hợp với Java API.
- **Embedding**: Cohere API (`embed-multilingual-v3.0`) — không cần GPU.
- **LLM**: Google Gemini API (`gemini-2.5-flash`) — miễn phí cao.
- **Vector DB**: FAISS (lưu trữ local, không cần server riêng).

```bash
cd Backend/ai-booking
python -m venv venv
venv\Scripts\activate     # Windows
pip install -r requirements.txt

# Lần đầu: Nạp dữ liệu vào FAISS index
python scripts/ingest.py

# Khởi động server
uvicorn app.main:app --reload --port 8000
```
> 💡 AI FastAPI Server sẽ hoạt động tại: `http://localhost:8000`
> 
> ⚠️ Cần có `GEMINI_API_KEY` và `COHERE_API_KEY` được khai báo trong file `Backend/ai-booking/.env`.

---

## 🛠 Lưu Ý Cấu Hình

- 🔒 **Bảo mật:** Không bao giờ đẩy tài nguyên nhạy cảm như `.env`, `google-services.json` hay `service-account.json` lên Git. Đảm bảo cấu hình `.gitignore` của bạn loại bỏ chúng hoàn toàn.
- 🗄️ **Supabase / PostgreSQL:** Khi cài DB trên các môi trường mới, phải bật tính năng vector vì Backend yêu cầu cho khối tìm kiếm AI:
  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```
- 📜 **Đóng góp Code:** Trước khi tạo module mới, hãy luôn tham chiếu `AGENTS.md` (chuẩn Naming, Layer). Cài đặt `GEMINI.md` để Agent có thể code hộ bạn theo đúng setup Local.

<br>

<div align="center">
  <sub>⭐️ Nếu bạn thấy dự án hữu ích, đừng quên cho 1 sao (star)! ⭐️</sub><br/>
  <sub>Developed by <b>Nova Ticket Team</b> ❤️</sub>
</div>