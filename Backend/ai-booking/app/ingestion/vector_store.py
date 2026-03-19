"""
app/ingestion/vector_store.py
ChromaDB — lưu local dưới dạng file, không cần cài DB riêng.
"""

import chromadb
from chromadb.config import Settings as ChromaSettings
from .embedder import EmbeddingModel
from .chunker import Chunk


class ChromaEmbeddingFunction:
    """Adapter để ChromaDB dùng embedding model của mình"""

    def __init__(self, embedder: EmbeddingModel):
        self.embedder = embedder

    def name(self) -> str:
        return f"custom_{self.embedder.model_name.replace('/', '_')}"

    def __call__(self, input: list[str]) -> list[list[float]]:
        return self.embedder.embed_batch(input)

    def embed_query(self, input: str) -> list[float]:
        return self.embedder.embed_query(input)

    def embed_documents(self, input: list[str]) -> list[list[float]]:
        return self.embedder.embed_batch(input)


class VectorStore:

    def __init__(self, embedder: EmbeddingModel, persist_dir: str = "./chroma_db",
                 collection_name: str = "nova_knowledge"):
        self.embedder = embedder
        self.collection_name = collection_name

        # Persistent client — lưu file xuống disk
        self.client = chromadb.PersistentClient(
            path=persist_dir,
            settings=ChromaSettings(anonymized_telemetry=False)
        )
        self.ef = ChromaEmbeddingFunction(embedder)
        self._get_or_create_collection()

    def _get_or_create_collection(self):
        self.collection = self.client.get_or_create_collection(
            name=self.collection_name,
            embedding_function=self.ef,
            metadata={"hnsw:space": "cosine"}   # cosine similarity
        )

    def reset_collection(self):
        """Xóa sạch collection cũ để nạp lại từ đầu"""
        try:
            self.client.delete_collection(self.collection_name)
        except Exception:
            pass
        self._get_or_create_collection()
        print(f"   ✓ Đã xóa collection cũ: {self.collection_name}")

    def add_documents(self, chunks: list[Chunk], batch_size: int = 50):
        """Upsert chunks vào ChromaDB theo batch"""
        total = len(chunks)
        for i in range(0, total, batch_size):
            batch = chunks[i: i + batch_size]
            self.collection.upsert(
                ids=[c.chunk_id for c in batch],
                documents=[c.content for c in batch],
                metadatas=[c.metadata for c in batch]
            )
            print(f"   ✓ Upserted {min(i + batch_size, total)}/{total} chunks")

    def search(self, query: str, n_results: int = 5,
               doc_type: str = None) -> list[dict]:
        """
        Tìm kiếm semantic trong ChromaDB.
        doc_type: lọc theo loại tài liệu (policy, faq, cinema_info, promotion)
        """
        where = {"doc_type": doc_type} if doc_type else None
        
        # Tự embed để đảm bảo định dạng (list của list - batch)
        query_emb = self.embedder.embed_query(query)

        results = self.collection.query(
            query_embeddings=[query_emb],
            n_results=n_results,
            where=where,
            include=["documents", "metadatas", "distances"]
        )

        hits = []
        for doc, meta, dist in zip(
            results["documents"][0],
            results["metadatas"][0],
            results["distances"][0]
        ):
            hits.append({
                "content":  doc,
                "metadata": meta,
                "score":    round(1 - dist, 4)  # cosine: 1=giống nhất
            })

        # Lọc kết quả quá xa (score thấp = ít liên quan)
        return [h for h in hits if h["score"] > 0.3]

    def count(self) -> int:
        return self.collection.count()
