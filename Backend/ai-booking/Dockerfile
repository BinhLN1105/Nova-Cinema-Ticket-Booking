FROM python:3.11-slim

# ── CẤU HÌNH BIẾN MÔI TRƯỜNG (Bắt buộc để xem log trên Cloud)
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# 1. Cài dependencies (Siêu nhẹ vì dùng FAISS CPU + API)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 2. Copy toàn bộ source và data
COPY app/ ./app/
COPY scripts/ ./scripts/
COPY data/ ./data/

# 3. BAKING: Nạp dữ liệu vào FAISS Index ngay trong lúc build
# Khai báo ARG để hứng giá trị từ --build-arg.
# ARG chỉ tồn tại trong quá trình build, KHÔNG bị lưu thành layer vào image cuối.
ARG COHERE_API_KEY
# Truyền thẳng ARG vào lệnh RUN — key sẽ không bị bake vào docker history
RUN mkdir -p faiss_index && COHERE_API_KEY=$COHERE_API_KEY python scripts/ingest.py

# 4. 🛡️ BẢO MẬT: Tạo user giới hạn quyền, không chạy bằng Root
RUN useradd --create-home --shell /bin/bash appuser \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

# 5. Khởi động server
# GEMINI_API_KEY và COHERE_API_KEY được inject lúc chạy container qua -e flag
# (hoặc cấu hình Environment Variables trên Azure / Render)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]