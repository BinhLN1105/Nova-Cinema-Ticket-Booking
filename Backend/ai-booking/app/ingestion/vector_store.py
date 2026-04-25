"""
app/ingestion/vector_store.py
FAISS — Lưu trữ vector dạng file cục bộ (thay thế ChromaDB để tương thích Windows).
"""

import os
import shutil
from langchain_community.vectorstores import FAISS
from .embedder import EmbeddingModel
from .chunker import Chunk


class VectorStore:

    def __init__(self, embedder: EmbeddingModel, persist_dir: str = "./faiss_index",
                 collection_name: str = "nova_knowledge"):
        self.embedder = embedder
        self.persist_dir = persist_dir
        # Lưu ý: FAISS không dùng collection_name như Chroma, nhưng ta giữ biến để compatible
        self.collection_name = collection_name
        self.vector_db = None

    def load(self) -> bool:
        """Load FAISS index từ disk lên RAM"""
        try:
            if os.path.exists(os.path.join(self.persist_dir, "index.faiss")):
                self.vector_db = FAISS.load_local(
                    folder_path=self.persist_dir,
                    embeddings=self.embedder.client,
                    allow_dangerous_deserialization=True
                )
                return True
        except Exception as e:
            print(f"   ⚠️ Lỗi load FAISS: {str(e)}")
        return False

    def reset_collection(self):
        """Xóa sạch index cũ trên disk và RAM"""
        if os.path.exists(self.persist_dir):
            shutil.rmtree(self.persist_dir)
        self.vector_db = None
        print(f"   ✓ Đã xóa index cũ tại: {self.persist_dir}")

    def add_documents(self, chunks: list[Chunk]):
        """Thêm chunks vào FAISS và lưu xuống disk ngay lập tức"""
        texts = [c.content for c in chunks]
        metadatas = [c.metadata for c in chunks]

        if self.vector_db is None:
            # Nếu chưa có index trên RAM, thử load từ disk xem có không
            if not self.load():
                # Nếu disk cũng không có, tạo mới hoàn toàn
                self.vector_db = FAISS.from_texts(
                    texts=texts,
                    embedding=self.embedder.client,
                    metadatas=metadatas
                )
            else:
                self.vector_db.add_texts(texts, metadatas=metadatas)
        else:
            self.vector_db.add_texts(texts, metadatas=metadatas)

        # FAISS bắt buộc phải gọi save_local để lưu lại file pkl/faiss
        self.vector_db.save_local(self.persist_dir)
        print(f"   ✓ Đã lưu {len(chunks)} chunks vào FAISS index")

    def search(self, query: str, n_results: int = 5) -> list[dict]:
        """Tìm kiếm semantic"""
        if self.vector_db is None:
            if not self.load():
                return []

        # similarity_search_with_score trả về list các (Document, score)
        # Với FAISS, score thường là L2 distance (càng nhỏ càng giống)
        results = self.vector_db.similarity_search_with_score(query, k=n_results)

        hits = []
        for doc, score in results:
            # Chuyển đổi distance sang độ tin cậy tương đối (0-1)
            # Vì FAISS L2 distance có thể > 1, ta dùng công thức đơn giản
            confidence = round(1 / (1 + score), 4)
            
            hits.append({
                "content":  doc.page_content,
                "metadata": doc.metadata,
                "score":    confidence
            })

        # Lọc kết quả ít liên quan
        return [h for h in hits if h["score"] > 0.3]

    def count(self) -> int:
        if self.vector_db is None:
            if not self.load():
                return 0
        return len(self.vector_db.docstore._dict)
