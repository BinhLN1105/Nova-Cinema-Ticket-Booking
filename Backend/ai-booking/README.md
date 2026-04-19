# NovaTicket AI Chatbot Service

Server AI chatbot cho ứng dụng đặt vé xem phim NovaTicket, tích hợp với Java Spring Boot backend thông qua mô hình RAG (Retrieval-Augmented Generation).

## Kiến trúc hệ thống

```
Frontend (React / Android App)
    │  POST /api/v1/chatbot/chat
    ▼
Java Spring Boot (Backend chính)
    │  POST /api/v1/chat  ← xác thực JWT rồi proxy sang Python
    ▼
Python AI Service (server này)
    ├── Tool 1: FAISS Vector DB (chính sách, FAQ, thông tin rạp)
    |             └── Embedding bởi Cohere API (embed-multilingual-v3.0)
    └── Tool 2: Java Internal API (lịch chiếu, ghế, voucher)
    
    LLM Response: Google Gemini API (gemini-2.5-flash)
```

## Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Framework | FastAPI + Uvicorn |
| LLM | Google Gemini API (`gemini-2.5-flash`) |
| Embedding | Cohere API (`embed-multilingual-v3.0`) — Cloud, không cần GPU |
| Vector DB | FAISS (`faiss-cpu`) — Lưu local, không cần server |
| Agent | LangChain (Tool-based Agent + Conversation Memory) |
| Language | Python 3.10+ |

> ✅ **Không cần GPU, không cần Docker, không tốn hàng GB dung lượng.**
> Kiến trúc dùng Cohere API để embedding và Gemini API để generate, giảm kích thước deploy từ ~7GB xuống còn **< 100MB**.

---

## Cấu trúc project

```
ai-booking/
├── app/
│   ├── main.py                # FastAPI app, định nghĩa endpoints
│   ├── config.py              # Settings (đọc từ .env)
│   ├── ingestion/
│   │   ├── loader.py          # Đọc file .md/.txt/.json/.csv
│   │   ├── chunker.py         # Tách văn bản thành chunks
│   │   ├── embedder.py        # Cohere Embedding API wrapper
│   │   └── vector_store.py    # FAISS wrapper (save/load local)
│   └── agent/
│       ├── tools.py           # Tool 1 (RAG) + Tool 2 (Java API)
│       └── chatbot.py         # LangChain Agent + Conversation Memory
├── scripts/
│   └── ingest.py              # Script nạp dữ liệu vào FAISS (chạy độc lập)
├── data/                      # File tĩnh — commit vào git
│   ├── policies/              # Chính sách thanh toán, hoàn vé
│   ├── faq/                   # Câu hỏi thường gặp
│   └── cinema_info/           # Thông tin rạp, giá vé
├── faiss_index/               # FAISS Vector DB (tự tạo, KHÔNG commit)
├── .env                       # Biến môi trường (KHÔNG commit)
├── .env.example               # Template môi trường
├── requirements.txt
└── Dockerfile
```

---

## Setup từ đầu (step-by-step)

### Bước 1: Tạo môi trường ảo và cài dependencies

```bash
cd Backend/ai-booking
python -m venv venv

# Windows:
venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
```

---

### Bước 2: Cấu hình .env

```bash
cp .env.example .env
```

Mở `.env` và điền các giá trị bắt buộc:

```env
# ── LLM (Google Gemini) ──────────────────────────────
GEMINI_API_KEY=your-key-here        # Lấy miễn phí tại: aistudio.google.com
LLM_MODEL=gemini-2.5-flash

# ── Embedding (Cohere) ───────────────────────────────
COHERE_API_KEY=your-key-here        # Lấy miễn phí tại: cohere.com
EMBEDDING_MODEL=embed-multilingual-v3.0

# ── Vector DB ────────────────────────────────────────
VECTOR_DB_DIR=./faiss_index

# ── Java Backend ─────────────────────────────────────
JAVA_API_BASE=http://localhost:8080
INTERNAL_API_KEY=nova-secret-2026   # Tự đặt, phải trùng với Spring Boot

# ── Security ─────────────────────────────────────────
JWT_SECRET=same-as-java-jwt-secret  # Copy từ application.properties Java
```

---

### Bước 3: Nạp dữ liệu vào Vector DB (FAISS)

Chạy script này một lần trước khi khởi động server, hoặc mỗi khi cập nhật file trong `data/`:

```bash
python scripts/ingest.py
```

Kết quả mong đợi:
```
=======================================================
  NovaTicket RAG — Data Ingestion
=======================================================
📂 Đọc file từ: data
   ✓ cinema_info\cinemas.md → 4 đoạn
   ✓ faq\general_faq.md → 2 đoạn
   ✓ policies\payment_policy.md → 4 đoạn
   ✓ Đọc được 10 tài liệu

✂️  Chunking (size=400, overlap=50)
   ✓ Tạo được 10 chunks

🔢 Embedding với Cohere API: embed-multilingual-v3.0
💾 Lưu vào FAISS index tại: ./faiss_index
✅ Hoàn thành! Index đã được lưu tại: ./faiss_index
```

---

### Bước 4: Khởi động server

```bash
# Development (tự reload khi code thay đổi)
uvicorn app.main:app --reload --port 8000

# Production
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 2
```

> 💡 AI FastAPI Server hoạt động tại: `http://localhost:8000`

---

### Bước 5: Kiểm tra hoạt động

```bash
# Health check
curl http://localhost:8000/health

# Test chat trực tiếp
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": "test_1", "user_message": "Sinh viên có được giảm giá không?"}'
```

---

## Luồng hoạt động thực tế

```
User: "Phim Lật Mặt 8h tối nay rạp CGV còn ghế không?"

1. Frontend → POST /api/v1/chatbot/chat (Java)
2. Java xác thực JWT, lấy userId → proxy sang Python
3. Python Agent nhận câu hỏi, quyết định:
   - "Đây là câu hỏi về lịch chiếu → dùng Tool 2 (Java API)"
4. Agent gọi get_showtimes("Lật Mặt", "CGV", today)
   → Java trả: [{id: 456, startTime: "20:00", availableSeats: 15}]
5. Agent tổng hợp → Gemini API tạo câu trả lời tự nhiên
6. Reply → Java → Frontend → User ✓
```

---

## Cập nhật dữ liệu kiến thức

Khi cần thêm/sửa chính sách, FAQ hoặc thông tin rạp:

```bash
# 1. Sửa file trong thư mục data/
# 2. Chạy lại ingest để cập nhật FAISS index
python scripts/ingest.py

# Hoặc gọi API sync từ Java admin panel
curl -X POST http://localhost:8000/api/v1/sync \
  -H "X-Internal-Key: nova-secret-2026"
```

---

## Xử lý sự cố thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `FAISS index not found` | Chưa chạy ingest | `python scripts/ingest.py` |
| `ResourceExhausted (429) Gemini` | Hết quota LLM | Chờ 1 phút hoặc Nova sang **Safe Mode** |
| `Cohere 401 Unauthorized` | API key sai | Kiểm tra `COHERE_API_KEY` trong `.env` |
| `Connection refused: localhost:8080` | Java chưa chạy | Start Java server trước |
| `Invalid internal API key` | Key không khớp | Kiểm tra `.env` và `application.properties` |

---

## Tính năng nâng cao

### 🛡️ Chế độ Fallback (Safe Mode)
Khi Gemini API đạt giới hạn (429), Nova tự động chuyển sang Safe Mode:
- Truy vấn trực tiếp từ FAISS (RAG thô, không qua LLM).
- Lấy danh sách phim đang chiếu từ Java API.
- Phản hồi nhanh kèm ghi chú tình trạng hệ thống.

### 💬 Conversation Memory
Agent giữ lịch sử hội thoại theo `session_id`, cho phép người dùng hỏi tiếp các câu liên quan mà không cần lặp lại ngữ cảnh.

### ✨ Rich Format Response
Phản hồi hỗ trợ đầy đủ Markdown: bảng giá, danh sách, chữ đậm — hiển thị đẹp trên giao diện chat của app.
