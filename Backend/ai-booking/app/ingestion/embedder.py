"""
app/ingestion/embedder.py
Embedding model chạy local — dùng sentence-transformers (free, hỗ trợ tiếng Việt)
"""

from sentence_transformers import SentenceTransformer


class EmbeddingModel:

    def __init__(self, model_name: str = "intfloat/multilingual-e5-base"):
        """
        multilingual-e5-base: ~500MB, hỗ trợ 100 ngôn ngữ gồm tiếng Việt.
        Download tự động lần đầu, cache lại sau.
        """
        print(f"   Loading embedding model: {model_name}")
        self.model = SentenceTransformer(model_name)
        self.model_name = model_name

    def embed(self, input: str) -> list[float]:
        """Embed 1 đoạn text"""
        # multilingual-e5 cần prefix "query:" khi search, "passage:" khi index
        return self.model.encode(f"passage: {input}", normalize_embeddings=True).tolist()

    def embed_query(self, input: str) -> list[float]:
        """Embed câu hỏi của user (dùng prefix 'query:' khác với indexing)"""
        # Nếu input là chuỗi, trả về list (D,)
        return self.model.encode(f"query: {input}", normalize_embeddings=True).tolist()

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """Embed nhiều đoạn cùng lúc — nhanh hơn gọi từng cái"""
        # Nếu đã có prefix thì giữ nguyên, không thì mặc định là passage:
        prefixed = []
        for t in texts:
            if t.startswith("query: ") or t.startswith("passage: "):
                prefixed.append(t)
            else:
                prefixed.append(f"passage: {t}")
        
        # Luôn đảm bảo encode nhận list và trả về list của list (N, D)
        return self.model.encode(prefixed, normalize_embeddings=True, batch_size=32).tolist()
