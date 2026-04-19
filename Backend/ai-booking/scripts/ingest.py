"""
scripts/ingest.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Script chạy độc lập — KHÔNG nằm trong luồng chat của user.

Dùng khi:
  - Lần đầu setup server
  - Khi team cập nhật file chính sách/FAQ
  - Chạy định kỳ (ví dụ mỗi đêm 2h qua cron)

Chạy:
  python scripts/ingest.py
  python scripts/ingest.py --dir data/policies  # chỉ sync 1 thư mục
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

import sys, argparse, time
from pathlib import Path

# Thêm root vào path để import được app.*
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.ingestion.loader import DocumentLoader
from app.ingestion.chunker import TextChunker
from app.ingestion.embedder import EmbeddingModel
from app.ingestion.vector_store import VectorStore
from app.config import get_settings

cfg = get_settings()


def run_ingestion(data_dir: str = "data"):
    print("=" * 55)
    print("  NovaTicket RAG — Data Ingestion")
    print("=" * 55)

    start = time.time()

    # 1. Load tất cả file
    print(f"\n📂 Đọc file từ: {data_dir}")
    loader = DocumentLoader(data_dir)
    raw_docs = loader.load_all()
    print(f"   ✓ Đọc được {len(raw_docs)} tài liệu")

    # 2. Chunk
    print(f"\n✂️  Chunking (size={cfg.chunk_size}, overlap={cfg.chunk_overlap})")
    chunker = TextChunker(cfg.chunk_size, cfg.chunk_overlap)
    chunks = chunker.split_documents(raw_docs)
    print(f"   ✓ Tạo được {len(chunks)} chunks")

    # 3. Embed + lưu vào ChromaDB
    print(f"\n🔢 Embedding với model: {cfg.embedding_model}")
    print("   (Lần đầu sẽ download model ~500MB, chờ 1–2 phút...)")
    embedder = EmbeddingModel(cfg.embedding_model)

    print(f"\n💾 Lưu vào FAISS index tại: {cfg.vector_db_dir}")
    store = VectorStore(embedder, persist_dir=cfg.vector_db_dir)
    store.reset_collection()   # Xóa cũ, nạp lại hoàn toàn
    store.add_documents(chunks)

    elapsed = time.time() - start
    print(f"\n{'=' * 55}")
    print(f"  ✅ Hoàn thành trong {elapsed:.1f}s")
    print(f"  📊 {len(chunks)} chunks đã lưu vào FAISS index")
    print(f"  📁 Index tại: {cfg.vector_db_dir}")
    print("=" * 55)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Ingest static data into ChromaDB")
    parser.add_argument("--dir", default="data", help="Thư mục data (mặc định: data/)")
    args = parser.parse_args()
    run_ingestion(args.dir)
