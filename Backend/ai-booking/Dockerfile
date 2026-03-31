FROM python:3.11-slim

# ── CẤU HÌNH BIẾN MÔI TRƯỜNG (Bắt buộc phải có để xem được Log trên Cloud)
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# 1. Cài dependencies trước để tối ưu Cache
COPY requirements.txt .
# Thêm build-essential nếu ChromaDB/hnswlib yêu cầu biên dịch C++
RUN apt-get update && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir -r requirements.txt

# 2. Copy toàn bộ source và data
COPY app/ ./app/
COPY scripts/ ./scripts/
COPY data/ ./data/

# 3. CHẠY INGEST TRONG LÚC BUILD (BAKING)
# Khi Render build image, nó sẽ chạy lệnh này tạo ra data trong chroma_db.
# Sau khi build xong, database này sẽ được "đóng băng" cứng vào trong Image.
# Khi Container khởi chạy, nó đã có sẵn DB, không cần chạy lại nữa!
RUN mkdir -p chroma_db && python scripts/ingest.py

# 🛡️ BẢO MẬT: Tạo user giới hạn quyền, không chạy bằng Root
RUN useradd --create-home --shell /bin/bash appuser \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

# 4. CHẠY SERVER NGAY LẬP TỨC
# Container khởi động lên là chỉ việc chạy Uvicorn, port mở trong tích tắc (chưa tới 1 giây).
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]