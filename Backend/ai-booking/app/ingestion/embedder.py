"""
app/ingestion/embedder.py
Embedding model via Cohere API — cực nhẹ, không cần tải model về local.
"""

from langchain_cohere import CohereEmbeddings
from ..config import get_settings

cfg = get_settings()


class EmbeddingModel:

    def __init__(self, model_name: str = "embed-multilingual-v3.0"):
        """
        Dùng model Cohere embed-multilingual-v3.0 qua API.
        """
        print(f"   Using Cohere Embedding API: {model_name}")
        self.model_name = model_name
        self.client = CohereEmbeddings(
            model=model_name,
            cohere_api_key=cfg.cohere_api_key
        )

    def embed_query(self, input: str) -> list[float]:
        """Embed câu hỏi của user"""
        return self.client.embed_query(input)

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """Embed nhiều đoạn cùng lúc (cho indexing)"""
        # Cohere tự xử lý batching và tối ưu phía API
        return self.client.embed_documents(texts)

    def embed(self, input: str) -> list[float]:
        """Alias cho embed_query hoặc đơn lẻ"""
        return self.embed_query(input)
