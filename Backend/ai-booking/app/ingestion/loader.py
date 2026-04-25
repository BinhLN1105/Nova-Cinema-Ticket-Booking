"""
app/ingestion/loader.py
Đọc tất cả định dạng file tĩnh → Document chuẩn hoá
"""

import json, csv
from pathlib import Path
from dataclasses import dataclass, field


@dataclass
class RawDocument:
    """Schema thống nhất cho mọi nguồn dữ liệu"""
    content:  str
    metadata: dict = field(default_factory=dict)
    # metadata chứa: source (path), doc_type, file_type


class DocumentLoader:

    SUPPORTED = {".md", ".txt", ".json", ".csv"}

    def __init__(self, data_dir: str = "data"):
        self.data_dir = Path(data_dir)

    def load_all(self) -> list[RawDocument]:
        docs = []
        for path in sorted(self.data_dir.rglob("*")):
            if path.suffix not in self.SUPPORTED:
                continue
            try:
                loaded = self._load_file(path)
                docs.extend(loaded)
                print(f"   ✓ {path.relative_to(self.data_dir)} → {len(loaded)} đoạn")
            except Exception as e:
                print(f"   ✗ Lỗi đọc {path}: {e}")
        return docs

    def _load_file(self, path: Path) -> list[RawDocument]:
        doc_type = self._infer_doc_type(path)
        meta_base = {"source": str(path), "doc_type": doc_type, "file_type": path.suffix}

        if path.suffix == ".md":
            return self._load_markdown(path, meta_base)
        elif path.suffix == ".txt":
            return self._load_text(path, meta_base)
        elif path.suffix == ".json":
            return self._load_json(path, meta_base)
        elif path.suffix == ".csv":
            return self._load_csv(path, meta_base)
        return []

    def _load_markdown(self, path: Path, meta: dict) -> list[RawDocument]:
        """
        Split theo heading ## → mỗi section là 1 document.
        Giúp RAG tìm đúng section hơn là trả về cả file.
        """
        text = path.read_text(encoding="utf-8")
        sections = []
        current_title = path.stem
        current_lines = []

        for line in text.splitlines():
            if line.startswith("## "):
                # Lưu section trước
                if current_lines:
                    sections.append((current_title, "\n".join(current_lines).strip()))
                current_title = line.lstrip("# ").strip()
                current_lines = []
            elif line.startswith("# "):
                # H1 = tiêu đề file, bỏ qua không tạo section riêng
                pass
            else:
                current_lines.append(line)

        # Section cuối
        if current_lines:
            sections.append((current_title, "\n".join(current_lines).strip()))

        return [
            RawDocument(
                content=f"[{title}]\n{body}",
                metadata={**meta, "section": title}
            )
            for title, body in sections if body
        ]

    def _load_text(self, path: Path, meta: dict) -> list[RawDocument]:
        """Split theo dòng trống → mỗi đoạn văn là 1 document"""
        text = path.read_text(encoding="utf-8")
        paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
        return [RawDocument(content=p, metadata=meta) for p in paragraphs]

    def _load_json(self, path: Path, meta: dict) -> list[RawDocument]:
        """
        Hỗ trợ 2 format:
        - List of objects → mỗi object là 1 document
        - Single object → 1 document
        """
        data = json.loads(path.read_text(encoding="utf-8"))
        items = data if isinstance(data, list) else [data]
        docs = []
        for i, item in enumerate(items):
            # Chuyển dict thành text mô tả tự nhiên để LLM dễ đọc
            content = self._dict_to_text(item)
            docs.append(RawDocument(
                content=content,
                metadata={**meta, "index": i}
            ))
        return docs

    def _load_csv(self, path: Path, meta: dict) -> list[RawDocument]:
        """Mỗi row CSV → 1 document dạng "key: value | key: value" """
        docs = []
        with open(path, encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for i, row in enumerate(reader):
                # Bỏ qua row trống
                if not any(row.values()):
                    continue
                content = " | ".join(f"{k}: {v}" for k, v in row.items() if v)
                docs.append(RawDocument(
                    content=content,
                    metadata={**meta, "row": i + 1}
                ))
        return docs

    def _dict_to_text(self, obj: dict, indent: int = 0) -> str:
        """Đệ quy chuyển dict → text dễ đọc"""
        lines = []
        prefix = "  " * indent
        for k, v in obj.items():
            if isinstance(v, dict):
                lines.append(f"{prefix}{k}:")
                lines.append(self._dict_to_text(v, indent + 1))
            elif isinstance(v, list):
                lines.append(f"{prefix}{k}: {', '.join(str(x) for x in v)}")
            else:
                lines.append(f"{prefix}{k}: {v}")
        return "\n".join(lines)

    def _infer_doc_type(self, path: Path) -> str:
        """Suy ra loại tài liệu từ tên thư mục"""
        folder = path.parent.name
        return {
            "policies":    "policy",
            "faq":         "faq",
            "cinema_info": "cinema_info",
            "promotions":  "promotion",
        }.get(folder, "general")
