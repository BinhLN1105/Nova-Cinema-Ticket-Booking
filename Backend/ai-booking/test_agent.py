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
    
    def request(self, method, url, *args, **kwargs):
        if method.lower() == "get":
            return self.get(url, *args, **kwargs)
        elif method.lower() == "post":
            return self.post(url, *args, **kwargs)
        else:
            resp = MagicMock()
            resp.status_code = 200
            resp.json.return_value = {}
            return resp

    def get(self, url, *args, **kwargs):
        params = kwargs.get("params", {})
        resp = MagicMock()
        resp.status_code = 200
        resp.raise_for_status = lambda: None
        
        if "/internal/api/showtimes" in url:
            params = kwargs.get("params", {})
            date_param = params.get("date")
            from datetime import datetime, timedelta
            today_str = datetime.now().strftime("%Y-%m-%d")
            tomorrow_str = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
            
            movie_title = params.get("movieTitle", "Mai")
            # Giả lập: Phim Mai hết suất hôm nay (trả về []). Còn các trường hợp khác trả về có suất.
            if movie_title == "Mai" and (date_param == today_str or not date_param):
                resp.json.return_value = []
            else:
                resp.json.return_value = [
                    {
                        "id": "125",
                        "cinemaName": "Nguyễn Trãi",
                        "screenName": "Phòng 1",
                        "screenType": "2D",
                        "startTime": f"{date_param or tomorrow_str}T20:00:00",
                        "endTime": f"{date_param or tomorrow_str}T22:00:00",
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
        elif "/internal/api/ai/user/tickets" in url:
            resp.json.return_value = {
                "status": "success",
                "message": "Tra cứu lịch sử vé đặt thành công",
                "data": {
                    "tickets": [
                        {
                            "bookingId": "BK-999",
                            "movieTitle": "Mai",
                            "cinemaName": "Nguyễn Trãi",
                            "startTime": "20:00",
                            "seats": ["G7", "G8"],
                            "status": "PAID"
                        }
                    ],
                    "cinePoints": 3137,
                    "rank": "Bạc"
                }
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

    # 1.5. Kiểm tra bóc tách ngày extract_date (Option 3)
    print("\n[1.5] Testing extract_date helper:")
    from app.agent.engines.template_engine import extract_date
    from datetime import datetime, timedelta
    
    t_str, is_exp = extract_date("lịch chiếu hôm nay")
    assert t_str == datetime.now().strftime("%Y-%m-%d")
    assert is_exp is True
    
    t_str, is_exp = extract_date("lịch chiếu ngày mai")
    assert t_str == (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    assert is_exp is True
    
    t_str, is_exp = extract_date("lịch chiếu phim Mai")
    assert t_str == datetime.now().strftime("%Y-%m-%d")
    assert is_exp is False
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
    assert "suat" in remove_non_ascii(response_showtime).lower(), f"Loi hien thi lich chieu: {remove_non_ascii(response_showtime)}"
    print("-> OK")

    # 5. Kiểm tra luồng trạng thái Slot Filling đặt vé & Combo bắp nước
    print("\n[5] Testing Slot Filling Booking Flow:")
    session_flow_id = "user_flow_uuid_99"

    # Case A1: Quy trình khởi tạo đặt vé trống (được hỏi chọn phim)
    step_init = chat(session_flow_id, "Đặt vé")["reply"]
    print(f"\nUser: Dat ve (init guide)\nNova clean:\n{remove_non_ascii(step_init)}")
    assert "phim nao" in remove_non_ascii(step_init).lower(), "Loi huong dan chon phim"

    # Case A2: Chưa có showtime_list trong state, gõ thẳng "Đăt vé suất 1" khi đang chờ phim
    step_empty = chat(session_flow_id, "Đặt vé suất 1")["reply"]
    print(f"\nUser: Dat ve suat 1 (empty state)\nNova clean:\n{remove_non_ascii(step_empty)}")
    assert "chua luu bo nho lich chieu" in remove_non_ascii(step_empty).lower(), "Loi fallback thieu lich chieu"

    # Bước A: Khởi tạo/Query lịch chiếu qua việc nhập phim
    step_query = chat(session_flow_id, "phim Mai")["reply"]
    print(f"\nUser: phim Mai\nNova clean:\n{remove_non_ascii(step_query)}")
    assert "suat" in remove_non_ascii(step_query).lower(), "Loi hien thi lich chieu sau khi nhap phim"

    # Case B: Tồn tại showtime_list nhưng nhập sai index (ngoài khoảng: 8)
    step_out_bounds = chat(session_flow_id, "Đặt vé suất 8")["reply"]
    print(f"\nUser: Dat ve suat 8 (out-of-bounds)\nNova clean:\n{remove_non_ascii(step_out_bounds)}")
    assert "khong ton tai" in remove_non_ascii(step_out_bounds).lower(), f"Loi validate out-of-bounds: {remove_non_ascii(step_out_bounds)}"

    # Case B0 (General query while showtime list present)
    step_general = chat(session_flow_id, "r sao đặt vé")["reply"]
    print(f"\nUser: r sao dat ve (general query when list exists)\nNova clean:\n{remove_non_ascii(step_general)}")
    assert "go" in remove_non_ascii(step_general).lower() or "suat" in remove_non_ascii(step_general).lower(), "Loi huong dan index khi da co list"

    # Case B0.5 (Selecting index using digit only "1")
    step_digit = chat(session_flow_id, "1")["reply"]
    print(f"\nUser: 1 (selecting index via single digit)\nNova clean:\n{remove_non_ascii(step_digit)}")
    assert "de xuat" in remove_non_ascii(step_digit).lower(), "Loi chon bang so don le"

    # Bước B: Chọn suất số 1 chuẩn (sẽ giải mã tự động ra id "125")
    step_a = chat(session_flow_id, "Đặt vé suất 1")["reply"]
    print(f"\nUser: Dat ve suat 1 (valid index)\nNova clean:\n{remove_non_ascii(step_a)}")
    assert "de xuat" in remove_non_ascii(step_a).lower(), "Thieu goi y ghe ngoi"

    # Bước C: Chọn ghế
    step_b = chat(session_flow_id, "chọn ghế G7 G8")["reply"]
    print(f"\nUser: chon ghe G7 G8\nNova clean:\n{remove_non_ascii(step_b)}")
    assert "combo" in remove_non_ascii(step_b).lower() or "bap nuoc" in remove_non_ascii(step_b).lower(), "Thieu buoc goi y bap nuoc"

    # Bước D: Chọn Combo bắp nước (Chọn Solo Combo)
    step_c = chat(session_flow_id, "thêm 1 combo solo")["reply"]
    print(f"\nUser: them 1 combo solo\nNova clean:\n{remove_non_ascii(step_c)}")
    assert "thanh cong" in remove_non_ascii(step_c).lower() or "nhap" in remove_non_ascii(step_c).lower(), "Loi dat combo"
    print("-> OK")

    # Case E: State Reset & RAG Fallback check
    print("\n[5.5] Testing State Reset & RAG Fallback:")
    session_reset_id = "user_reset_uuid_10"
    chat(session_reset_id, "Đặt vé")
    chat(session_reset_id, "phim Mai")
    chat(session_reset_id, "1")
    
    step_reset = chat(session_reset_id, "phim Daredevil: Tái Sinh 2")["reply"]
    print(f"\nUser: phim Daredevil: Tai Sinh 2\nNova clean:\n{remove_non_ascii(step_reset)}")
    assert "chua luu bo nho" not in remove_non_ascii(step_reset).lower(), "Loi: Khong reset duoc state ve nhap khi hoi phim moi"
    assert "danh sach" in remove_non_ascii(step_reset).lower() or "mai" in remove_non_ascii(step_reset).lower(), "Loi lay lai movies list khi rag not found"
    print("-> OK")

    # 6. Kiểm tra toàn bộ từ khóa không chứa dấu tiếng Việt gốc
    print("\n[6] Testing keyword accent-free validation:")
    for intent, kws in classifier.keywords.items():
        for kw in kws:
            assert kw == remove_vietnamese_accents(kw), f"Keyword sai lech dau: '{kw}' trong intent {intent}"
    print("-> OK")

    # 7. Kiểm tra tra cứu Hạng thành viên & Lịch sử vé (USER_QUERIES)
    print("\n[7] Testing User Profile and Tickets Query:")
    user_queries_response = chat(session_id, "Tôi muốn kiểm tra thẻ thành viên và lịch sử vé")["reply"]
    print(f"\nUser: Toi muon kiem tra the thanh vien va lich su ve\nNova clean:\n{remove_non_ascii(user_queries_response)}")
    assert "bac" in remove_non_ascii(user_queries_response).lower(), "Thieu thong tin Hang Bac"
    assert "3137" in remove_non_ascii(user_queries_response).lower(), "Thieu thong tin 3137 CinePoint"
    assert "bk-999" in remove_non_ascii(user_queries_response).lower(), "Thieu thong tin tickets gan day"
    print("-> OK")

    print("\n=== COMPLETE TESTING AI AGENT SUCCESSFULLY ===")

if __name__ == "__main__":
    run_tests()
