"""
app/ingestion/chunker.py
Chia nhỏ Document thành chunks để embedding.
"""

from dataclasses import dataclass, field
from .loader import RawDocument


@dataclass
class Chunk:
    """Một đoạn nhỏ sẵn sàng để embed"""
    content:  str
    metadata: dict = field(default_factory=dict)
    chunk_id: str  = ""


class TextChunker:

    def __init__(self, chunk_size: int = 400, overlap: int = 50):
        self.chunk_size = chunk_size
        self.overlap    = overlap

    def split_documents(self, docs: list[RawDocument]) -> list[Chunk]:
        chunks = []
        
        for doc in docs:
            doc_chunks = self._split_text(doc.content)
            source = doc.metadata.get("source", "unknown")
            
            for text in doc_chunks:
                # Dùng chính chiều dài hiện tại của mảng làm ID
                # Lần đầu mảng rỗng -> len = 0. Lần hai -> len = 1... tăng dần đều!
                current_id = len(chunks)
                
                chunks.append(Chunk(
                    content=text,
                    metadata=doc.metadata,
                    chunk_id=f"{source}::chunk_{current_id}"
                ))
                
        return chunks

    def _split_text(self, text: str) -> list[str]:
        """
        Split theo từ với overlap.
        Ưu tiên split tại dấu câu (. ? !) để chunk tự nhiên hơn.
        """
        if len(text.split()) <= self.chunk_size:
            return [text]  # Đủ nhỏ rồi, không cần split

        sentences = self._split_sentences(text)
        chunks: list[str] = []
        current_words: list[str] = []
        current_count = 0

        for sentence in sentences:
            s_words = sentence.split()
            s_count = len(s_words)

            if current_count + s_count > self.chunk_size and current_words:
                # Lưu chunk hiện tại
                chunks.append(" ".join(current_words))
                # Overlap: giữ lại một phần cuối
                start_overlap = max(0, len(current_words) - self.overlap)
                overlap_words: list[str] = []
                for i in range(start_overlap, len(current_words)):
                    overlap_words.append(current_words[i])
                
                # Reset and extend
                next_words: list[str] = []
                next_words.extend(overlap_words)
                next_words.extend(s_words)
                current_words = next_words
                current_count = len(current_words)
            else:
                current_words.extend(s_words)
                current_count += s_count

        if current_words:
            chunks.append(" ".join(current_words))

        return chunks if chunks else [text]

    def _split_sentences(self, text: str) -> list[str]:
        """Split tại dấu câu và xuống dòng"""
        import re
        # Split tại . ! ? và \n\n, giữ lại dấu câu
        parts = re.split(r'(?<=[.!?])\s+|\n\n+', text)
        return [p.strip() for p in parts if p.strip()]
