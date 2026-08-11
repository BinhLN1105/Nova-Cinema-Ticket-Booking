import os
import sys
from unittest.mock import MagicMock, patch

# Thêm directory hiện tại vào sys.path để import app.xxx
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Định hình môi trường mock trước khi import
os.environ["USE_MOCK_AI"] = "true"
os.environ["GEMINI_API_KEY"] = ""

# Import hook tự động stub mọi langchain.* sub-module để tránh ModuleNotFoundError
import importlib
from types import ModuleType
from importlib.abc import MetaPathFinder, Loader

class _LangchainStubLoader(Loader):
    def create_module(self, spec):
        m = ModuleType(spec.name)
        m.__path__ = []
        m.__package__ = spec.name
        
        # Để module tự động giải quyết các class/function/sub-module chưa định nghĩa thành MagicMock
        def __getattr__(name):
            if name == "tool":
                return lambda f: f
            if name == "BaseTool":
                return object
            return MagicMock
        m.__getattr__ = __getattr__
        return m
    def exec_module(self, module):
        pass

class _LangchainStubFinder(MetaPathFinder):
    # Chỉ stub các module thực mà chatbot/app import, không stub các attribute lá sâu hơn
    _MODULES = {
        "langchain",
        "langchain_google_genai",
        "langchain_cohere",
        "langchain_community",
        "cohere"
    }
    def find_spec(self, fullname, path, target=None):
        for m in self._MODULES:
            if fullname == m or fullname.startswith(m + "."):
                spec = importlib.util.spec_from_loader(fullname, _LangchainStubLoader())
                return spec
        return None

sys.meta_path.insert(0, _LangchainStubFinder())

# Import httpx trước để monkey-patch trước khi app.xxx được import
import httpx as _httpx_module

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
        elif method.lower() == "delete":
            return self.delete(url, *args, **kwargs)
        else:
            resp = MagicMock()
            resp.status_code = 200
            resp.json.return_value = {}
            return resp

    def get(self, url, *args, **kwargs):
        params = kwargs.get("params", {})
        from datetime import datetime, timedelta
        tomorrow_str = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
        resp = MagicMock()
        resp.status_code = 200
        resp.raise_for_status = lambda: None
        
        if "/internal/api/movies/now-showing" in url:
            resp.json.return_value = [
                {"id": "mov1", "title": "Mai"},
                {"id": "mov2", "title": "Kung Fu Panda 4"},
                {"id": "mov3", "title": "Daredevil: Tái Sinh 2"}
            ]
        elif "/internal/api/showtimes" in url:
            resp.json.return_value = [
                {
                    "id": "125",
                    "movieTitle": "Mai",
                    "cinemaName": "Nguyễn Trãi",
                    "screenName": "Phòng 1",
                    "screenType": "2D",
                    "startTime": f"{tomorrow_str}T20:00:00",
                    "endTime": f"{tomorrow_str}T22:00:00",
                    "availableSeats": 42
                },
                {
                    "id": "126",
                    "movieTitle": "Daredevil: Tái Sinh 2",
                    "cinemaName": "Nguyễn Trãi",
                    "screenName": "Phòng 1",
                    "screenType": "2D",
                    "startTime": f"{tomorrow_str}T20:00:00",
                    "endTime": f"{tomorrow_str}T22:00:00",
                    "availableSeats": 42
                }
            ]
        elif "/internal/api/seats/available" in url:
            resp.json.return_value = {
                "success": True,
                "status": "success",
                "data": {
                    "availableVipSeats": 12,
                    "availableStandardSeats": 30,
                },
                "totalSeats": 100,
                "availableSeats": 42,
                "availableVipSeats": 12,
                "availableStandardSeats": 30,
                "availableCoupleSeats": 0,
                "seats": [
                    {"showtimeSeatId": "seat1", "rowLabel": "G", "colNumber": 7, "seatLabel": "G7"},
                    {"showtimeSeatId": "seat2", "rowLabel": "G", "colNumber": 8, "seatLabel": "G8"},
                    {"showtimeSeatId": "seat3", "rowLabel": "H", "colNumber": 6, "seatLabel": "H6"},
                    {"showtimeSeatId": "seat4", "rowLabel": "H", "colNumber": 7, "seatLabel": "H7"}
                ]
            }
        elif "/internal/api/ai/user/tickets" in url:
            resp.json.return_value = {
                "status": "success",
                "message": "Tra cứu lịch sử vé đặt thành công",
                "data": {
                    "tickets": [
                        {
                            "bookingCode": "BK-999",
                            "showtimeId": "125",
                            "movieTitle": "Mai",
                            "cinemaName": "Nguyễn Trãi",
                            "startTime": f"{tomorrow_str}T20:00:00",
                            "seats": ["G7", "G8"],
                            "status": "PAID"
                        }
                    ],
                    "cinePoints": 3137,
                    "rank": "Bạc"
                }
            }
        elif "/internal/api/ai/reminder/list" in url:
            resp.json.return_value = {
                "success": True,
                "data": [
                    {
                        "id": "rem-aaa-111",
                        "title": "Nhắc đặt vé phim Mai",
                        "body": "Đến thời gian đặt vé phim Mai của suất chiếu rồi anh/chị ơi!",
                        "createdAt": f"{tomorrow_str}T20:00:00"
                    },
                    {
                        "id": "rem-bbb-222",
                        "title": "Nhắc giờ chiếu Daredevil",
                        "body": "Đến giờ xem phim Daredevil của suất chiếu rồi anh/chị ơi!",
                        "createdAt": f"{tomorrow_str}T21:00:00"
                    }
                ]
            }
        elif "/internal/api/ai/weather/showtime" in url:
            showtime_id = url.split("/")[-1]
            if showtime_id == "125":
                resp.json.return_value = {
                    "success": True,
                    "status": "success",
                    "data": {
                        "condition": "Mưa giông lớn",
                        "temperature": 26.5,
                        "isBadWeather": True,
                        "outOfForecastRange": False,
                        "warningMessage": "Dự báo thời tiết lúc 20:00 tại rạp Nguyễn Trãi sẽ có mưa giông lớn 🌧️. Anh/chị nên mang theo áo mưa hoặc đi sớm chút để tránh tắc đường nhé!"
                    }
                }
            elif showtime_id == "126":
                resp.json.return_value = {
                    "success": True,
                    "status": "success",
                    "data": {
                        "condition": "Nhiều mây",
                        "temperature": 29.0,
                        "isBadWeather": False,
                        "outOfForecastRange": False,
                        "warningMessage": ""
                    }
                }
            elif showtime_id == "999":
                resp.json.return_value = {
                    "success": True,
                    "status": "success",
                    "data": {
                        "condition": None,
                        "temperature": None,
                        "isBadWeather": False,
                        "outOfForecastRange": True,
                        "warningMessage": ""
                    }
                }
            elif showtime_id == "777":
                resp.json.return_value = {
                    "success": True,
                    "status": "success",
                    "data": {
                        "condition": None,
                        "temperature": None,
                        "isBadWeather": False,
                        "outOfForecastRange": False,
                        "warningMessage": ""
                    }
                }
            else: # 888 hoặc mặc định API error
                resp.status_code = 500
                resp.json.return_value = {
                    "success": False,
                    "status": "error",
                    "message": "Weather API failed"
                }
        elif "/api/v1/combos" in url:
            resp.json.return_value = {
                "status": "success",
                "data": [
                    {"id": "combo-solo-uuid-111", "name": "Solo Combo", "price": 65000},
                    {"id": "combo-couple-uuid-222", "name": "Couple Combo", "price": 90000}
                ]
            }
        else:
            resp.json.return_value = []
        return resp

    def delete(self, url, *args, **kwargs):
        resp = MagicMock()
        resp.status_code = 200
        resp.raise_for_status = lambda: None
        if "/internal/api/ai/reminder/" in url:
            resp.json.return_value = {"success": True, "message": "Xóa nhắc lịch thành công"}
        else:
            resp.json.return_value = {}
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
        elif "/internal/api/ai/reminder/draft" in url:
            resp.json.return_value = {
                "success": True,
                "status": "success",
                "message": "Đã cài đặt nhắc nhở lịch xem phim thành công",
                "data": {
                    "reminderId": "d3b07384-d113-4ec6-a192-3c35bba3f02e",
                    "showtimeId": json_data.get("showtimeId", "125"),
                    "reminderTime": "2026-07-24T20:00:00",
                    "reminderType": json_data.get("reminderType", "SHOWTIME")
                }
            }
        else:
            resp.json.return_value = {}
        return resp

# Monkey-patch httpx.Client toàn cục - hiệu quả với mọi module đã import httpx ở top-level
_httpx_module.Client = MockClient

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
    from app.agent.state import session_manager
    print(f"\n[DEBUG] current state in session: {session_manager.get_state(session_flow_id)}")
    step_digit = chat(session_flow_id, "1")["reply"]
    print(f"\n[DEBUG] step_digit returned: {step_digit}")
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
    assert "danh sach" in remove_non_ascii(step_reset).lower() or "mai" in remove_non_ascii(step_reset).lower() or "daredevil" in remove_non_ascii(step_reset).lower(), "Loi lay lai movies list khi rag not found"
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

    # 8. Kiểm thử luồng nhắc lịch 2 bước mới, validation & cancellation (Phase 15)
    print("\n[8] Testing Phase 15 Reminder Flow, Validation & Cancellation:")
    session_rem_id = "user_reminder_uuid_100"
    
    # 8.1. Khởi tạo luồng nhắc lịch
    res_init = chat(session_rem_id, "Tôi muốn đặt nhắc lịch")["reply"]
    print(f"\nUser: Toi muon dat nhac lich\nNova clean:\n{remove_non_ascii(res_init)}")
    assert "chon loai hinh nhac lich" in remove_non_ascii(res_init).lower(), "Loi chon loai hinh nhac lich"
    
    # 8.2. Nhập sai loại (validation fallback)
    res_invalid_flow = chat(session_rem_id, "sai bet")["reply"]
    print(f"\nUser: sai bet\nNova clean:\n{remove_non_ascii(res_invalid_flow)}")
    assert "anh/chi chi can chon 1" in remove_non_ascii(res_invalid_flow).lower(), "Loi validation chon loai"
    
    # 8.3 & 8.4. Kiểm thử Hủy tiến trình hoạt động (Cancellation)
    res_cancel = chat(session_rem_id, "Hủy lịch")["reply"]
    print(f"\nUser: Huy lich\nNova clean:\n{remove_non_ascii(res_cancel)}")
    assert "huy tien trinh cai dat nhac lich" in remove_non_ascii(res_cancel).lower(), "Loi cancellation luong hoat dong"
    state_rem = session_manager.get_state(session_rem_id)
    assert state_rem.get("current_step") is None
    
    # 8.5. Khởi tạo lại và đi theo Option 1: Nhắc đặt vé (BOOKING)
    chat(session_rem_id, "Tôi muốn đặt nhắc lịch")
    res_opt1 = chat(session_rem_id, "1")["reply"]
    print(f"\nUser: 1\nNova clean:\n{remove_non_ascii(res_opt1)}")
    assert "phim nao" in remove_non_ascii(res_opt1).lower(), "Loi hoi ten phim option 1"
    
    # Thử nghiệm so khớp chuẩn hóa phim không dấu + dấu câu (ví dụ: "daredevil tai sinh 2")
    res_norm = chat(session_rem_id, "daredevil tai sinh 2")["reply"]
    print(f"\nUser: daredevil tai sinh 2\nNova clean:\n{remove_non_ascii(res_norm)}")
    assert "da ghi nhan phim" in remove_non_ascii(res_norm).lower() and "daredevil" in remove_non_ascii(res_norm).lower(), "Loi normalized movie match"
    
    # Không giới hạn rạp (chọn "Không") -> Hoàn thành đặt nhắc lịch và ẩn UUID ở ID nhắc lịch
    res_success_booking = chat(session_rem_id, "Không")["reply"]
    print(f"\nUser: Khong\nNova clean:\n{remove_non_ascii(res_success_booking)}")
    assert "da tao nhac nho" in remove_non_ascii(res_success_booking).lower(), "Loi tao nhac nho booking"
    assert "khi suat chieu chuan bi mo ban" in remove_non_ascii(res_success_booking).lower(), "Loi thoi gian nhac nho booking"
    assert "mã nhắc nhở" not in res_success_booking.lower(), "Loi hien ma nhac nho UUID"
    
    # Check status clean
    state_rem = session_manager.get_state(session_rem_id)
    assert state_rem.get("current_step") is None
    
    # 8.6. Khởi tạo lại và kiểm thử Option 2: Nhắc suất chiếu (SHOWTIME - Thư viện vé của người dùng)
    chat(session_rem_id, "Tôi muốn đặt nhắc lịch")
    res_opt2 = chat(session_rem_id, "2")["reply"]
    print(f"\nUser: 2\nNova clean:\n{remove_non_ascii(res_opt2)}")
    assert "danh sach ve chuan bi chieu" in remove_non_ascii(res_opt2).lower(), "Loi hiển thị danh sách vé đã mua"
    
    # Chọn suất chiếu index 1 -> Hoàn thành nhắc lịch chiếu 1 tiếng
    res_success_showtime = chat(session_rem_id, "1")["reply"]
    print(f"\nUser: 1\nNova clean:\n{remove_non_ascii(res_success_showtime)}")
    assert "da tao nhac nho" in remove_non_ascii(res_success_showtime).lower(), "Loi tao nhac nho showtime"
    assert "1 tieng truoc suat chieu" in remove_non_ascii(res_success_showtime).lower(), "Loi thoi gian nhac nho showtime 1 tieng"
    
    # 8.7. Kiểm thử clarify_movie khi có nhiều candidates khớp trong luồng nhắc lịch
    session_clarify_id = "user_clarify_uuid_300"
    chat(session_clarify_id, "Tôi muốn đặt nhắc lịch")
    chat(session_clarify_id, "1")
    # Gõ "ai" khớp cả "Mai" và "Daredevil: Tái Sinh 2" (giả lập so khớp substring)
    res_clarify_prompt = chat(session_clarify_id, "ai")["reply"]
    print(f"\nUser: ai\nNova clean:\n{remove_non_ascii(res_clarify_prompt)}")
    assert "chon so thu tu phim" in remove_non_ascii(res_clarify_prompt).lower(), "Loi trigger clarify movie"
    
    # Nhập index sai -> validation error
    res_clarify_invalid = chat(session_clarify_id, "5")["reply"]
    print(f"\nUser: 5\nNova clean:\n{remove_non_ascii(res_clarify_invalid)}")
    assert "khong hop le" in remove_non_ascii(res_clarify_invalid).lower(), "Loi validate clarify index"
    
    # Nhập index đúng -> chọn phim [1]=Daredevil (sort alpha) -> chuyển sang hỏi rạp
    res_clarify_valid = chat(session_clarify_id, "1")["reply"]
    print(f"\nUser: 1\nNova clean:\n{remove_non_ascii(res_clarify_valid)}")
    # candidates = ['Daredevil: Tái Sinh 2', 'Mai'] (sort alpha) -> 1 = Daredevil
    assert "daredevil" in remove_non_ascii(res_clarify_valid).lower(), "Loi chon phim tu clarify"
    
    # Check 0 matches fallback
    chat(session_clarify_id, "Không")
    chat(session_clarify_id, "Tôi muốn đặt nhắc lịch")
    chat(session_clarify_id, "1")
    res_zero = chat(session_clarify_id, "phim ngon tinh chau a")["reply"]
    print(f"\nUser: phim ngon tinh chau a\nNova clean:\n{remove_non_ascii(res_zero)}")
    assert "khong tim thay phim" in remove_non_ascii(res_zero).lower(), "Loi 0 match fallback"
    print("-> OK (Reminder flow tests pass)")

    # 9. Kiểm thử luồng Quản lý nhắc lịch - Option 3 (Phase 15.5)
    print("\n[9] Testing Reminder Management (Option 3 - Phase 15.5):")
    session_mgmt_id = "user_mgmt_uuid_500"

    # 9.1. Vào luồng nhắc lịch và chọn Option 3
    res_init3 = chat(session_mgmt_id, "Tôi muốn quản lý nhắc lịch")["reply"]
    print(f"\nUser: quan ly nhac lich\nNova clean:\n{remove_non_ascii(res_init3)}")
    assert "chon loai hinh nhac lich" in remove_non_ascii(res_init3).lower(), "Loi: phai hoi loai hinh nhac lich"

    res_opt3 = chat(session_mgmt_id, "3")["reply"]
    print(f"\nUser: 3\nNova clean:\n{remove_non_ascii(res_opt3)}")
    assert "danh sach nhac lich" in remove_non_ascii(res_opt3).lower(), "Loi: phai hien danh sach nhac lich"
    assert "nhac dat ve phim mai" in remove_non_ascii(res_opt3).lower(), "Loi: thieu item '1' trong danh sach"
    assert "nhac gio chieu daredevil" in remove_non_ascii(res_opt3).lower(), "Loi: thieu item '2' trong danh sach"

    # 9.2. Nhập index không hợp lệ → fallback, state không reset
    res_invalid_idx = chat(session_mgmt_id, "9")["reply"]
    print(f"\nUser: 9\nNova clean:\n{remove_non_ascii(res_invalid_idx)}")
    assert "so thu tu khong hop le" in remove_non_ascii(res_invalid_idx).lower(), "Loi: phai fallback index khong hop le"
    state_mgmt = session_manager.get_state(session_mgmt_id)
    assert state_mgmt.get("current_step") == "showtime_flow_cancel_reminder_confirm", "Loi: state phai giu nguyen sau invalid index"

    # 9.3. Chọn index hợp lệ [1] → hiện confirm
    res_confirm = chat(session_mgmt_id, "1")["reply"]
    print(f"\nUser: 1\nNova clean:\n{remove_non_ascii(res_confirm)}")
    assert "chac chan" in remove_non_ascii(res_confirm).lower(), "Loi: phai hoi xac nhan yes/no"
    assert "nhac dat ve phim mai" in remove_non_ascii(res_confirm).lower(), "Loi: phai show ten nhac lich can huy"

    # 9.4. Gõ sai ở bước verify → fallback không reset state
    res_verify_invalid = chat(session_mgmt_id, "uku")["reply"]
    print(f"\nUser: uku\nNova clean:\n{remove_non_ascii(res_verify_invalid)}")
    assert "co" in remove_non_ascii(res_verify_invalid).lower() and "khong" in remove_non_ascii(res_verify_invalid).lower(), "Loi: phai nhac nhap co/khong"
    state_mgmt2 = session_manager.get_state(session_mgmt_id)
    assert state_mgmt2.get("current_step") == "showtime_flow_cancel_reminder_verify", "Loi: state phai giu nguyen sau invalid verify"

    # 9.5. Xác nhận 'Có' → xóa thành công
    res_delete_ok = chat(session_mgmt_id, "Có")["reply"]
    print(f"\nUser: Co\nNova clean:\n{remove_non_ascii(res_delete_ok)}")
    assert "da huy nhac lich" in remove_non_ascii(res_delete_ok).lower(), "Loi: phai thong bao da huy thanh cong"
    state_clean = session_manager.get_state(session_mgmt_id)
    assert state_clean.get("current_step") is None, "Loi: state phai reset sau khi huy nhac lich"

    # 9.6. Luồng bulk delete - 'Tất cả'
    session_bulk_id = "user_bulk_uuid_600"
    chat(session_bulk_id, "Tôi muốn quản lý nhắc lịch")
    chat(session_bulk_id, "3")
    res_bulk_confirm = chat(session_bulk_id, "Tất cả")["reply"]
    print(f"\nUser: Tat ca\nNova clean:\n{remove_non_ascii(res_bulk_confirm)}")
    assert "toan bo nhac lich" in remove_non_ascii(res_bulk_confirm).lower(), "Loi: phai xac nhan xoa toan bo"

    res_bulk_yes = chat(session_bulk_id, "Có")["reply"]
    print(f"\nUser: Co\nNova clean:\n{remove_non_ascii(res_bulk_yes)}")
    assert "da huy toan bo nhac lich" in remove_non_ascii(res_bulk_yes).lower(), "Loi: phai thong bao huy toan bo thanh cong"
    state_bulk_clean = session_manager.get_state(session_bulk_id)
    assert state_bulk_clean.get("current_step") is None, "Loi: state phai reset sau bulk delete"

    # 9.7. Chọn 'Không' → giữ nguyên nhắc lịch
    session_no_id = "user_no_uuid_700"
    chat(session_no_id, "Tôi muốn quản lý nhắc lịch")
    chat(session_no_id, "3")
    chat(session_no_id, "1")
    res_no = chat(session_no_id, "Không")["reply"]
    print(f"\nUser: Khong\nNova clean:\n{remove_non_ascii(res_no)}")
    assert "giu nguyen nhac lich" in remove_non_ascii(res_no).lower(), "Loi: phai giu nguyen nhac lich khi noi Khong"
    state_no_clean = session_manager.get_state(session_no_id)
    assert state_no_clean.get("current_step") is None, "Loi: state phai reset sau khi tu choi"

    print("-> OK (Reminder management tests pass)")

    # 10. Kiểm thử luồng Thời tiết suất chiếu (Phase 16)
    print("\n[10] Testing Weather Integration (Phase 16):")
    session_weather_id = "user_weather_uuid_800"

    # F1: Hỏi thời tiết khi chưa chọn suất chiếu
    res_w_none = chat(session_weather_id, "Thời tiết lúc chiếu phim thế nào?")["reply"]
    print(f"\nUser: Thoi tiet luc chieu phim the nao?\nNova clean:\n{remove_non_ascii(res_w_none)}")
    assert "chua ro" in remove_non_ascii(res_w_none).lower() or "tim kiem" in remove_non_ascii(res_w_none).lower(), "Loi: phai nhac nho chon suat chieu truoc"

    # F2: Tìm kiếm lịch chiếu (ghi nhận showtime_list có suất 125 rạp Nguyễn Trãi có mưa giông)
    res_showtimes = chat(session_weather_id, "lịch chiếu phim Mai ở rạp Nguyễn Trãi")["reply"]
    print(f"\nUser: lich chieu phim Mai o rap Nguyen Trai\nNova clean:\n{remove_non_ascii(res_showtimes)}")
    # Cảnh báo ngầm 🌧️ tự động in ra danh sách lịch chiếu
    assert "mua giong" in remove_non_ascii(res_showtimes).lower() or "🌧️" in res_showtimes, "Loi: khong tu dong hien canh bao thoi tiet xau"

    # F3: Hỏi thời tiết cho suất chiếu vừa tìm (suất 125 mặc định ở đầu list)
    res_w_explicit = chat(session_weather_id, "thời tiết hôm đó mưa không?")["reply"]
    print(f"\nUser: thoi tiet hom do mua khong?\nNova clean:\n{remove_non_ascii(res_w_explicit)}")
    assert "mua giong lon" in remove_non_ascii(res_w_explicit).lower() and "🌧️" in res_w_explicit, "Loi: phai tra loi canh bao thoi tiet mua giong lon cho suat 125"

    # F4: Gọi tool lay thoi tiet cho suat 777 (chưa set toa do / ko coordinates coords)
    from app.tools.weather_tools import GetShowtimeWeatherTool
    weather_tool = GetShowtimeWeatherTool()
    res_w_tool_777 = weather_tool.execute("777")
    assert res_w_tool_777.get("isBadWeather") is False
    assert res_w_tool_777.get("condition") is None

    # F5: Gọi tool lay thoi tiet cho suat 999 (out of range forecast)
    res_w_tool_999 = weather_tool.execute("999")
    assert res_w_tool_999.get("outOfForecastRange") is True
    # Kiểm tra chatbot response khi out of forecast range
    # Set showtime_list với một item id 999
    state_weather = session_manager.get_state(session_weather_id)
    state_weather["showtime_list"] = [{"id": "999", "movieTitle": "Phim Xa Xôi", "cinemaName": "Rạp Nguyễn Trãi"}]
    session_manager.set_state(session_weather_id, state_weather)
    res_w_out_range = chat(session_weather_id, "thời tiết hôm đó thế nào?")["reply"]
    print(f"\nUser: thoi tiet hom do the nao (out of range)?\nNova clean:\n{remove_non_ascii(res_w_out_range)}")
    assert "chua co du bao" in remove_non_ascii(res_w_out_range).lower(), "Loi: phai bao out of forecast range"

    # F6: Khi bot gap API error (suat 888) -> fallback an toan
    state_weather = session_manager.get_state(session_weather_id)
    state_weather["showtime_list"] = [{"id": "888", "movieTitle": "Phim Lỗi", "cinemaName": "Rạp Nguyễn Trãi"}]
    session_manager.set_state(session_weather_id, state_weather)
    res_w_err = chat(session_weather_id, "thời tiết hôm đó có giông bão không?")["reply"]
    print(f"\nUser: thoi tiet hom do co giong bao khong (API error)?\nNova clean:\n{remove_non_ascii(res_w_err)}")
    assert "loi ket noi" in remove_non_ascii(res_w_err).lower() or "khong the lay" in remove_non_ascii(res_w_err).lower(), "Loi fallback khi API thoi tiet loi"

    print("-> OK (Weather integration tests pass)")

    print("\n=== COMPLETE TESTING AI AGENT SUCCESSFULLY ===")

if __name__ == "__main__":
    run_tests()
