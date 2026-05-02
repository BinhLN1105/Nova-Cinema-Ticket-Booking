"""
app/agent/tools.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Định nghĩa 2 Tool cho AI Agent:

  Tool 1 — search_knowledge_base:
    Tìm trong ChromaDB (chính sách, FAQ, thông tin rạp)
    → Dùng khi: câu hỏi về chính sách, quy định, giá vé cố định

  Tool 2 — get_realtime_cinema_data:
    Gọi HTTP sang Java Spring Boot API
    → Dùng khi: hỏi lịch chiếu, ghế trống, voucher đang chạy
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

import json
import httpx
from langchain.tools import tool
from functools import lru_cache

from ..config import get_settings
from ..ingestion.embedder import EmbeddingModel
from ..ingestion.vector_store import VectorStore

cfg = get_settings()

# ── Singleton: khởi tạo 1 lần, dùng lại mãi ─────────────────
@lru_cache(maxsize=1)
def _get_vector_store() -> VectorStore:
    embedder = EmbeddingModel(cfg.embedding_model)
    return VectorStore(
        embedder,
        persist_dir=cfg.vector_db_dir,
        collection_name=cfg.chroma_collection
    )

def _java_headers() -> dict:
    return {"X-Internal-Key": cfg.internal_api_key}


# ════════════════════════════════════════════════════════════
#  TOOL 1 — Tìm kiếm kiến thức tĩnh (RAG)
# ════════════════════════════════════════════════════════════
@tool
def search_knowledge_base(query: str) -> str:
    """
    Tìm kiếm thông tin trong cơ sở kiến thức nội bộ của NovaTicket.
    Dùng tool này khi khách hỏi về:
    - Chính sách hoàn vé, thanh toán, VNPay
    - Ưu đãi sinh viên, người cao tuổi, trẻ em
    - Chương trình tích điểm NOVA Points
    - Thông tin rạp chiếu, giá vé cố định, combo bắp nước
    - Câu hỏi thường gặp (FAQ)
    - Lỗi kỹ thuật hoặc hướng dẫn sử dụng ứng dụng
    """
    try:
        store = _get_vector_store()
        results = store.search(query, n_results=4)

        if not results:
            return "Không tìm thấy thông tin liên quan trong cơ sở kiến thức."

        # Format kết quả thành text cho LLM dễ đọc
        parts = []
        for i, r in enumerate(results, 1):
            source = r["metadata"].get("source", "unknown")
            section = r["metadata"].get("section", "")
            header = f"[Nguồn {i}: {source}" + (f" — {section}]" if section else "]")
            parts.append(f"{header}\n{r['content']}")

        return "\n\n---\n\n".join(parts)

    except Exception as e:
        return f"Lỗi khi tìm kiếm: {str(e)}"


# ════════════════════════════════════════════════════════════
#  TOOL 2 — Lấy dữ liệu thời gian thực từ Java API
# ════════════════════════════════════════════════════════════
@tool
def get_now_showing_movies(genre: str = "") -> str:
    """
    Lấy danh sách phim đang chiếu tại tất cả rạp NovaTicket.
    Dùng tool này khi khách hỏi: "phim gì đang chiếu?", "có phim hành động không?",
    "tuần này chiếu phim gì?", "phim mới nhất là gì?".
    Tham số genre: lọc theo thể loại (hành động, tình cảm, hoạt hình...) — để trống = lấy tất cả.
    """
    try:
        params = {"genre": genre} if genre else {}
        with httpx.Client(timeout=10) as client:
            resp = client.get(
                f"{cfg.java_api_base}/internal/api/movies/now-showing",
                headers=_java_headers(), params=params
            )
            resp.raise_for_status()
            movies = resp.json()

        if not movies:
            return "Hiện tại không có phim nào đang chiếu."

        lines = ["Danh sách phim đang chiếu:"]
        for m in movies[:10]:   # Giới hạn 10 phim để không spam
            genres = ", ".join(g["name"] for g in m.get("genres", []))
            lines.append(
                f"• {m['title']} ({m.get('rated', 'N/A')}) — {m.get('duration', '?')} phút"
                f" — Thể loại: {genres or 'Đa dạng'}"
            )
        return "\n".join(lines)

    except httpx.HTTPStatusError as e:
        return f"Không lấy được danh sách phim (lỗi {e.response.status_code})."
    except Exception as e:
        return f"Lỗi kết nối: {str(e)}"


@tool
def get_showtimes(movie_title: str, cinema_name: str = "", date: str = "") -> str:
    """
    Lấy lịch chiếu của một bộ phim cụ thể.
    Dùng tool này khi khách hỏi: "phim X chiếu lúc mấy giờ?", "hôm nay có suất nào?",
    "cuối tuần này chiếu phim X ở đâu?", "suất chiếu phim X tại rạp Y".
    Tham số:
      - movie_title: tên phim (bắt buộc)
      - cinema_name: tên rạp (để trống = tất cả rạp)
      - date: ngày chiếu dạng DD/MM/YYYY (để trống = hôm nay)
    """
    try:
        params = {"movieTitle": movie_title}
        if cinema_name: params["cinemaName"] = cinema_name
        if date:        params["date"] = date

        with httpx.Client(timeout=10) as client:
            resp = client.get(
                f"{cfg.java_api_base}/internal/api/showtimes",
                headers=_java_headers(), params=params
            )
            resp.raise_for_status()
            showtimes = resp.json()

        if not showtimes:
            return f"Không tìm thấy suất chiếu nào cho phim '{movie_title}'" + \
                   (f" tại rạp '{cinema_name}'" if cinema_name else "") + \
                   (f" vào ngày {date}" if date else " hôm nay") + "."

        lines = [f"Lịch chiếu phim '{movie_title}':"]
        for s in showtimes:
            available_text = ""
            if "availableSeats" in s:
                seats = s["availableSeats"]
                if seats == 0:
                    available_text = " ⚠️ HẾT VÉ"
                elif seats < 10:
                    available_text = f" (còn {seats} ghế — sắp hết)"
                else:
                    available_text = f" (còn {seats} ghế)"

            lines.append(
                f"• {s['cinemaName']} — Phòng {s.get('screenName', '?')}"
                f" [{s.get('screenType', '2D')}]"
                f" — {s['startTime']} → {s['endTime']}"
                f"{available_text}"
                f" (Mã suất: {s['id']})"
            )
        return "\n".join(lines)

    except httpx.HTTPStatusError as e:
        return f"Không lấy được lịch chiếu (lỗi {e.response.status_code})."
    except Exception as e:
        return f"Lỗi kết nối: {str(e)}"


@tool
def get_available_seats(showtime_id: int) -> str:
    """
    Kiểm tra ghế trống của một suất chiếu cụ thể.
    Dùng tool này khi khách hỏi: "suất đó còn ghế không?", "ghế nào còn trống?",
    "có ghế đôi không?", "còn bao nhiêu ghế?", "ghế VIP còn không?".
    Tham số showtime_id: mã suất chiếu (lấy từ kết quả get_showtimes).
    """
    try:
        with httpx.Client(timeout=10) as client:
            resp = client.get(
                f"{cfg.java_api_base}/internal/api/seats/available",
                headers=_java_headers(),
                params={"showtimeId": showtime_id}
            )
            resp.raise_for_status()
            data = resp.json()

        total     = data.get("totalSeats", 0)
        available = data.get("availableSeats", 0)
        vip       = data.get("availableVipSeats", 0)
        couple    = data.get("availableCoupleSeats", 0)
        standard  = data.get("availableStandardSeats", 0)

        if available == 0:
            return f"Suất chiếu #{showtime_id} đã HẾT VÉ. Tổng {total} ghế đã đặt hết."

        lines = [
            f"Suất chiếu #{showtime_id} còn {available}/{total} ghế trống:",
            f"  • Ghế thường: {standard} ghế",
        ]
        if vip > 0:
            lines.append(f"  • Ghế VIP: {vip} ghế")
        if couple > 0:
            lines.append(f"  • Ghế đôi (Sweetbox): {couple} cặp")

        return "\n".join(lines)

    except httpx.HTTPStatusError as e:
        return f"Không lấy được thông tin ghế (lỗi {e.response.status_code})."
    except Exception as e:
        return f"Lỗi kết nối: {str(e)}"


@tool
def get_active_vouchers() -> str:
    """
    Lấy danh sách mã voucher đang hoạt động.
    Dùng tool này khi khách hỏi: "có mã giảm giá không?", "voucher hiện tại là gì?",
    "có khuyến mãi nào không?", "tôi được giảm giá bao nhiêu?".
    """
    try:
        with httpx.Client(timeout=10) as client:
            resp = client.get(
                f"{cfg.java_api_base}/internal/api/vouchers/active",
                headers=_java_headers()
            )
            resp.raise_for_status()
            vouchers = resp.json()

        if not vouchers:
            return "Hiện tại không có mã voucher nào đang hoạt động."

        lines = ["Mã voucher đang hoạt động:"]
        for v in vouchers:
            val = v["discountValue"]
            unit = "%" if v["discountType"] == "PERCENTAGE" else "đ"
            max_d = f" (tối đa {v['maxDiscount']:,}đ)" if v.get("maxDiscount") else ""
            lines.append(
                f"• [{v['code']}] Giảm {val}{unit}{max_d}"
                f" — Đơn từ {v.get('minOrder', 0):,}đ"
                f" — HSD: {v['endDate']}"
            )
        return "\n".join(lines)

    except Exception as e:
        return f"Lỗi kết nối: {str(e)}"


# Danh sách tools xuất ra để Agent dùng
ALL_TOOLS = [
    search_knowledge_base,
    get_now_showing_movies,
    get_showtimes,
    get_available_seats,
    get_active_vouchers,
]
