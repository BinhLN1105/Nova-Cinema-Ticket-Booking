# NovaTicket RAG Chatbot Server

Server AI chatbot cho ứng dụng đặt vé xem phim, tích hợp với Java Spring Boot backend.

## Kiến trúc

```
Frontend (React)
    │  POST /api/v1/chatbot/chat
    ▼
Java Spring Boot (MVC chính)
    │  POST /api/v1/chat  ← proxy sang Python
    ▼
Python RAG Server (server này)
    ├── Tool 1: ChromaDB (chính sách, FAQ, thông tin rạp)
    └── Tool 2: Java Internal API (lịch chiếu, ghế, voucher)
```

## Cấu trúc project

```
nova-rag/
├── app/
│   ├── main.py                # FastAPI app
│   ├── config.py              # Settings từ .env
│   ├── adapters/
│   ├── ingestion/
│   │   ├── loader.py          # Đọc md/txt/json/csv
│   │   ├── chunker.py         # Split thành chunks
│   │   ├── embedder.py        # Embedding model (local)
│   │   └── vector_store.py    # ChromaDB wrapper
│   └── agent/
│       ├── tools.py           # Tool 1 (RAG) + Tool 2 (API)
│       └── chatbot.py         # LangChain Agent + memory
├── scripts/
│   └── ingest.py              # Script nạp file tĩnh (chạy độc lập)
├── data/                      # File tĩnh — commit vào git
│   ├── policies/              # Chính sách thanh toán, hoàn vé
│   ├── faq/                   # Câu hỏi thường gặp
│   ├── cinema_info/           # Thông tin rạp, giá vé
│   └── promotions/            # Chương trình khuyến mãi
├── java_side/                 # Code Java cần thêm vào Spring Boot
│   ├── DataSyncController.java
│   ├── ChatbotProxyController.java
│   ├── AiChatService.java
│   └── application_additions.properties
├── chroma_db/                 # Vector DB (tự tạo, không commit)
├── .env                       # Cấu hình (không commit)
├── .env.example               # Template
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

---

## Setup từ đầu (step-by-step)

### Bước 1: Cài Python dependencies

```bash
cd nova-rag
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

> Lần đầu cài `sentence-transformers` sẽ download model ~500MB. Chỉ xảy ra 1 lần.

---

### Bước 2: Cấu hình .env

```bash
cp .env.example .env
```

Mở `.env` và điền:

```env
GEMINI_API_KEY=your-key-here          # Lấy từ aistudio.google.com (miễn phí)
JAVA_API_BASE=http://localhost:8080    # URL Java server của bạn
INTERNAL_API_KEY=nova-secret-2024     # Tự đặt, phải trùng với Java
JWT_SECRET=same-as-java-jwt-secret    # Copy từ application.properties Java
```

---

### Bước 3: Chuẩn bị file dữ liệu tĩnh

Đặt các file tài liệu vào thư mục `data/`:

```
data/
├── policies/
│   ├── payment_policy.md      ← Chính sách thanh toán, hoàn vé
│   └── refund_policy.md
├── faq/
│   └── general_faq.md         ← Câu hỏi thường gặp
├── cinema_info/
│   └── cinemas.md             ← Thông tin rạp, giá vé
└── promotions/
    └── seasonal_offers.json   ← Chương trình khuyến mãi
```

Format file hỗ trợ: `.md`, `.txt`, `.json`, `.csv`

---

### Bước 4: Nạp dữ liệu vào Vector DB

```bash
# Chạy script này một lần, hoặc mỗi khi cập nhật file tĩnh
python scripts/ingest.py

# Kết quả mong đợi:
# ===============================================
#   NovaTicket RAG — Data Ingestion
# ===============================================
# 📂 Đọc file từ: data
#    ✓ policies/payment_policy.md → 5 đoạn
#    ✓ faq/general_faq.md → 8 đoạn
#    ✓ cinema_info/cinemas.md → 4 đoạn
# ✓ Tạo được 34 chunks
# 💾 Lưu vào ChromaDB tại: ./chroma_db
# ✅ Hoàn thành trong 12.3s
```

---

### Bước 5: Thêm code vào Java Spring Boot

Copy các file trong `java_side/` vào project Java của bạn:

**1. `DataSyncController.java`** → `src/main/java/.../controller/`

**2. `ChatbotProxyController.java`** → `src/main/java/.../controller/`

**3. `AiChatService.java`** → `src/main/java/.../service/`

**4. Thêm vào `application.properties`:**
```properties
nova.python.rag.url=http://localhost:8000
nova.internal.api-key=nova-secret-2024     # Phải giống INTERNAL_API_KEY trong .env
```

**5. Thêm WebClient bean vào Spring Boot:**
```java
// Trong class @Configuration bất kỳ
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

**6. Thêm dependency vào `pom.xml`** (nếu chưa có WebFlux):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

### Bước 6: Khởi động Python server

```bash
# Development
uvicorn app.main:app --reload --port 8000

# Production
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 2
```

Hoặc dùng Docker:
```bash
docker-compose up -d
```

---

### Bước 7: Kiểm tra hoạt động

```bash
# Health check
curl http://localhost:8000/health

# Test chat trực tiếp (bypass Java)
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": "test_1", "user_message": "Sinh viên có được giảm giá không?"}'

# Kết quả mong đợi:
# {"reply": "Dạ có ạ! Sinh viên được giảm 15% giá vé thường...", "session_id": "test_1"}
```

---

## Luồng hoạt động thực tế

```
User: "Phim Mai 8h tối nay rạp Landmark còn ghế không?"

1. Frontend → POST /api/v1/chatbot/chat (Java)
2. Java xác thực JWT, lấy userId → gọi Python
3. Python Agent nhận câu hỏi, suy nghĩ:
   - "Đây là câu hỏi về ghế trống → dùng Tool 2"
4. Agent gọi get_showtimes("Mai", "Landmark", today)
   → Java trả: [{id: 456, startTime: "20:00", availableSeats: 15}]
5. Agent gọi get_available_seats(456)
   → Java trả: {available: 15, vip: 3, standard: 12}
6. Agent tổng hợp → trả lời:
   "Dạ suất 20:00 phim Mai tại Landmark 81 hiện còn 15 ghế ạ,
    trong đó có 3 ghế VIP và 12 ghế thường. Anh/chị muốn đặt không ạ?"
7. Java → Frontend → User ✓
```

---

## Cập nhật dữ liệu

### Khi team cập nhật file chính sách/FAQ:

```bash
# Cách 1: Chạy script thủ công
python scripts/ingest.py

# Cách 2: Gọi API từ Java admin panel
curl -X POST http://localhost:8000/api/v1/sync \
  -H "X-Internal-Key: nova-secret-2024"
```

### Thêm loại file mới:
Chỉ cần thả file vào thư mục `data/` đúng subfolder, chạy `ingest.py` lại.

---

| `ResourceExhausted (429)` | Hết quota Gemini | Chờ 1 phút hoặc Nova tự sang **Safe Mode** |
| `ChromaDB: collection not found` | Chưa chạy ingest | `python scripts/ingest.py` |
| `Connection refused: localhost:8080` | Java chưa chạy | Start Java server trước |
| `Invalid internal API key` | Key không khớp | Kiểm tra .env và application.properties |
| `Missing gemini-1.5-flash` | Model không khả dụng | Chuyển sang `gemini-2.0-flash-lite-001` trong .env |

---

## Tính năng nâng cao

### 🛡️ Chế độ Fallback (Safe Mode) - [chatbot.py](file:///d:/Project_Android-TicketBooking/Backend/ai-booking/app/agent/chatbot.py)
Khi LLM đạt tới giới hạn (429 Request), Nova sẽ tự động chuyển sang cấu hình Tra cứu thô:
- Truy vấn trực tiếp từ cơ sở kiến thức (RAG thô).
- Lấy danh sách phim đang chiếu từ Java API.
- Phản hồi nhanh chóng cho người dùng kèm ghi chú về tình trạng hệ thống.

### ✨ Hiển thị đẹp (Rich Format) - [AiChatbot.jsx](file:///d:/Project_Android-TicketBooking/Frontend/nova-ticketbooking/src/components/customer/AiChatbot.jsx)
Cửa sổ chat hỗ trợ đầy đủ các định dạng trình bày:
- **Bảng giá (Tables)**: Dễ dàng xem danh sách combo bắp nước, giá vé.
- **Danh sách (Lists)**: Liệt kê các chính sách, ưu đãi sinh viên rõ ràng.
- **Chữ đậm (Bold)**: Nhấn mạnh thông tin quan trọng như hotline, địa chỉ rạp.
