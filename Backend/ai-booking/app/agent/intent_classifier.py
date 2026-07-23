import re
import unicodedata
from ..config import get_settings

cfg = get_settings()

def remove_vietnamese_accents(input_str: str) -> str:
    """Loại bỏ hoàn toàn dấu tiếng Việt để so khớp keyword chính xác"""
    # Xử lý riêng chữ Đ / đ
    input_str = input_str.replace('đ', 'd').replace('Đ', 'D')
    # Phân rã composite unicode characters
    nfd_form = unicodedata.normalize('NFD', input_str)
    # Lọc bỏ dấu thanh (combining marks)
    return "".join([c for c in nfd_form if not unicodedata.combining(c)])

class IntentClassifier:
    def __init__(self):
        # Bộ từ khóa đại diện cho từng Intent sau khi đã xóa dấu
        self.keywords = {
            "GREETING": ["chao", "xin chao", "hi", "hello", "helo", "ban la ai", "tro ly", "bot", "advertiser"],
            "BOOKING_DRAFT": ["dat ve", "suat chieu", "suat", "suat ", "ghe ", "dat ghe", "mua ve", "giu ghe", "dat cho"],
            "REMINDER_DRAFT": ["nhac nho", "nhac lich", "hen gio", "dat hen", "bao gio chieu", "nhac nho lich"],
            "USER_QUERIES": ["lich su", "diem", "point", "cinepoint", "ve da mua", "da dat", "ve cua toi", "thẻ", "rank"],
            "KNOWLEDGE_RAG": ["hoan ve", "huy ve", "chinh sach", "gia ve", "bap nuoc", "combo", "vnpay", "thanh toan", "lien he", "dia chi"]
        }

    def classify(self, text: str) -> str:
        """Phân loại ý định của người dùng"""
        # 1. Normalizing văn bản đầu vào
        normal_text = text.lower().strip()
        clean_text = remove_vietnamese_accents(normal_text)

        # 2. Bước 1: Phân loại theo Regex/Từ khóa (Tốc độ cực nhanh và đáng tin cậy)
        for intent, kw_list in self.keywords.items():
            for kw in kw_list:
                # Sử dụng Word boundary \b cho từ khóa ngắn tránh trùng lặp chuỗi con (VD: "hi" trong "phim")
                if len(kw) <= 4:
                    pattern = rf"\b{re.escape(kw)}\b"
                else:
                    pattern = re.escape(kw)
                
                if re.search(pattern, clean_text):
                    # Đặc thù: Nếu có từ "lịch sử" hoặc "điểm" thì ưu tiên USER_QUERIES trước
                    if intent == "BOOKING_DRAFT" and any(x in clean_text for x in ["lich su", "ve cua toi", "da dat"]):
                        return "USER_QUERIES"
                    return intent

        # 3. Bước 2: Tự động Fallback sang RAG hỏi đáp nếu có từ khóa chính sách chung
        if any(x in clean_text for x in ["sao", "nhu nao", "huong dan", "quy dinh"]):
            return "KNOWLEDGE_RAG"

        # 4. Mặc định trả về KNOWLEDGE_RAG để hệ thống tra cứu cơ sở dữ liệu FAISS
        return "KNOWLEDGE_RAG"
