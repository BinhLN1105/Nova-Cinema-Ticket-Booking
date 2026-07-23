import os
import sys
from unittest.mock import MagicMock, patch

# Thêm directory hiện tại vào sys.path để import app.xxx
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Định hình môi trường mock trước khi import
os.environ["USE_MOCK_AI"] = "true"
os.environ["GEMINI_API_KEY"] = ""

# Giả lập httpx.Client để chạy test độc lập không cần Java API chạy thật
class MockClient:
    def __init__(self, *args, **kwargs):
        pass
    def __enter__(self):
        return self
    def __exit__(self, exc_type, exc_val, exc_tb):
        pass
    
    def get(self, url, *args, **kwargs):
        params = kwargs.get("params", {})
        resp = MagicMock()
        resp.status_code = 200
        resp.raise_for_status = lambda: None
        
        if "/internal/api/showtimes" in url:
            resp.json.return_value = [
                {
                    "id": "125",
                    "cinemaName": "Nguyễn Trãi",
                    "screenName": "Phòng 1",
                    "screenType": "2D",
                    "startTime": "20:00",
                    "endTime": "22:00",
                    "availableSeats": 42
                }
            ]
        elif "/internal/api/seats/available" in url:
            resp.json.return_value = {
                "totalSeats": 100,
                "availableSeats": 42,
                "availableVipSeats": 12,
                "availableStandardSeats": 30,
                "availableCoupleSeats": 0
            }
        else:
            resp.json.return_value = []
        return resp

    def post(self, url, *args, **kwargs):
        json_data = kwargs.get("json", {})
        resp = MagicMock()
        resp.status_code = 200
        resp.raise_for_status = lambda: None
        
        if "/internal/api/ai/booking/draft" in url:
            # Giá trị trả về khi tạo vé nháp
            resp.json.return_value = {
                "status": "success",
                "message": "Tạo đơn vé nháp thành công",
                "data": {
                    "draftId": "mock_draft_uuid_abc123",
                    "showtimeId": json_data.get("showtimeId", "125"),
                    "seats": json_data.get("seatIds", ["G7", "G8"]),
                    "totalAmount": 120000 + (65000 if json_data.get("combos") else 0),
                    "movieTitle": "Mai",
                    "startTime": "2026-07-24T20:00:00",
                    "cinemaName": "Nguyễn Trãi"
                }
            }
        else:
            resp.json.return_value = {}
        return resp

# Sử dụng patch để thay thế httpx.Client bằng MockClient trong suốt thời gian chạy test
@patch("httpx.Client", MockClient)
def run_tests():
    from app.agent.chatbot import chat
    from app.agent.intent_classifier import remove_vietnamese_accents

    print("=== START TESTING AI AGENT ===")
    
    # helper xoa dau de hien thi / assert
    def remove_non_ascii(text: str) -> str:
        clean = remove_vietnamese_accents(text)
        return clean.encode('ascii', errors='ignore').decode('ascii')

    # 1. Kiểm tra xóa dấu tiếng Việt
    print("\n[1] Testing remove_vietnamese_accents:")
    sample_text = "Chao ban! Toi muon dat ghe hang G bo phim Kung Fu Panda 4."
    clean_text = remove_non_ascii(sample_text)
    print(f"Original: {sample_text}")
    print(f"Stripped: {clean_text}")
    assert "chao ban" in clean_text.lower(), "Loi xoa dau tieng Viet"
    print("-> OK")

    # 2. Kiểm tra bộ phân loại ý định (IntentClassifier)
    print("\n[2] Testing IntentClassifier:")
    from app.agent.intent_classifier import IntentClassifier
    classifier = IntentClassifier()
    
    tests = {
        "Chào bot, giới thiệu đi": "GREETING",
        "Tôi muốn đặt vé bộ phim Mai": "BOOKING_DRAFT",
        "Nhắc nhở lịch xem phim cho mình lúc 20:00": "REMINDER_DRAFT",
        "hiển thị các vé đã mua giúp tôi": "USER_QUERIES",
        "quy định và chính sách hoàn vé như nào vậy": "KNOWLEDGE_RAG",
        "có bắp nước combo gì giảm giá không": "KNOWLEDGE_RAG"
    }
    
    for query, expected in tests.items():
        predicted = classifier.classify(query)
        safe_query = remove_non_ascii(query)
        print(f"Query: '{safe_query}' -> Predicted: {predicted} (Expected: {expected})")
        assert predicted == expected, f"Sai lech: '{safe_query}' ra {predicted}, mong doi {expected}"
    print("-> OK")

    # 3. Kiểm tra Proxy chatbot logic (Offline Template Engine)
    print("\n[3] Testing Chatbot Response Fallback:")
    
    session_id = "user_test_uuid_1"
    
    response_greeting = chat(session_id, "Hello bot!")["reply"]
    print(f"\nUser: Hello bot!\nNova:\n{remove_non_ascii(response_greeting)}")
    assert "Nova" in response_greeting, "Thieu tu khoa Nova trong hoi thoai chao mung"
    
    response_rag = chat(session_id, "Chinh sach hoan ve")["reply"]
    print(f"\nUser: Chinh sach hoan ve\nNova:\n{remove_non_ascii(response_rag)}")
    print("-> OK")

    # 4. Kiểm tra tra cứu lịch chiếu suất phim kèm lọc Rạp
    print("\n[4] Testing Showtime Queries:")
    response_showtime = chat(session_id, "lịch chiếu phim Mai ở rạp Nguyễn Trãi")["reply"]
    print(f"\nUser: lich chieu phim Mai o rap Nguyen Trai\nNova:\n{remove_non_ascii(response_showtime)}")
    print(f"DEBUG response_showtime raw: {response_showtime}")
    assert "suat" in remove_non_ascii(response_showtime).lower(), "Loi hien thi lich chieu"
    print("-> OK")

    # 5. Kiểm tra luồng trạng thái Slot Filling đặt vé & Combo bắp nước
    print("\n[5] Testing Slot Filling Booking Flow:")
    session_flow_id = "user_flow_uuid_99"

    # Bước A: Khởi tạo đặt vé bằng mã suất chiếu
    step_a = chat(session_flow_id, "Đặt vé suất 125")["reply"]
    print(f"\nUser: Dat ve suat 125\nNova:\n{remove_non_ascii(step_a)}")
    assert "de xuat" in remove_non_ascii(step_a).lower(), "Thieu goi y ghe ngoi"

    # Bước B: Chọn ghế
    step_b = chat(session_flow_id, "chọn ghế G7 G8")["reply"]
    print(f"\nUser: chon ghe G7 G8\nNova:\n{remove_non_ascii(step_b)}")
    assert "combo" in remove_non_ascii(step_b).lower() or "bap nuoc" in remove_non_ascii(step_b).lower(), "Thieu buoc goi y bap nuoc"

    # Bước C: Chọn Combo bắp nước (Chọn Solo Combo)
    step_c = chat(session_flow_id, "thêm 1 combo solo")["reply"]
    print(f"\nUser: them 1 combo solo\nNova:\n{remove_non_ascii(step_c)}")
    assert "thanh cong" in remove_non_ascii(step_c).lower() or "nhap" in remove_non_ascii(step_c).lower(), "Loi dat combo"
    print("-> OK")

    print("\n=== COMPLETE TESTING AI AGENT SUCCESSFULLY ===")

if __name__ == "__main__":
    run_tests()
