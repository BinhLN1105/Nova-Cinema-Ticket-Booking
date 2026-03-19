FROM python:3.11-slim

WORKDIR /app

# Cài dependencies trước (cache layer riêng)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source
COPY app/ ./app/
COPY scripts/ ./scripts/
COPY data/ ./data/

# Tạo thư mục ChromaDB
RUN mkdir -p chroma_db

EXPOSE 8000

# Chạy ingest lần đầu rồi start server
CMD ["sh", "-c", "python scripts/ingest.py && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
