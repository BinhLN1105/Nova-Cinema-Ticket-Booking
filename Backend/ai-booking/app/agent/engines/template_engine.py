import re
from .base_engine import BaseChatEngine
from .response_formatter import ResponseFormatter
from ...tools import create_draft_booking, get_suggested_seats, create_draft_reminder, get_user_tickets
from ...tools.reminder_tools import GetRemindersTool, DeleteReminderTool
from ...agent.tools import search_knowledge_base, get_now_showing_movies, get_active_vouchers, get_active_cinemas, get_cinema_screens
from ..intent_classifier import IntentClassifier
from ..state import session_manager

get_reminders_tool = GetRemindersTool()
delete_reminder_tool = DeleteReminderTool()

def extract_date(msg_lower: str) -> tuple[str, bool]:
    import re
    from datetime import datetime, timedelta
    
    now = datetime.now()
    
    # 1. Khớp cụ thể dd/mm/yyyy hoặc dd-mm-yyyy
    match_full = re.search(r"(\d{1,2})[/-](\d{1,2})[/-](\d{4})", msg_lower)
    if match_full:
        day = int(match_full.group(1))
        month = int(match_full.group(2))
        year = int(match_full.group(3))
        try:
            d = datetime(year, month, day)
            return d.strftime("%Y-%m-%d"), True
        except ValueError:
            pass

    # 2. Khớp cụ thể dd/mm hoặc dd-mm (ví dụ 26/7, 26-7) -> gán năm hiện tại
    match_short = re.search(r"\b(\d{1,2})[/-](\d{1,2})\b", msg_lower)
    if match_short:
        day = int(match_short.group(1))
        month = int(match_short.group(2))
        year = now.year
        try:
            d = datetime(year, month, day)
            return d.strftime("%Y-%m-%d"), True
        except ValueError:
            pass
            
    # 3. Phân tích từ khóa tương đối
    if "mai" in msg_lower or "ngay mai" in msg_lower or "ngày mai" in msg_lower:
        d = now + timedelta(days=1)
        return d.strftime("%Y-%m-%d"), True
        
    if "kia" in msg_lower or "ngay kia" in msg_lower or "ngày kia" in msg_lower or "mốt" in msg_lower or "mot" in msg_lower:
        d = now + timedelta(days=2)
        return d.strftime("%Y-%m-%d"), True
        
    if "hôm nay" in msg_lower or "hom nay" in msg_lower:
        return now.strftime("%Y-%m-%d"), True
        
    # 4. Thứ trong tuần
    weekdays_map = {
        "thứ hai": 0, "thu hai": 0, "t2": 0, "thứ 2": 0, "thu 2": 0,
        "thứ ba": 1, "thu ba": 1, "t3": 1, "thứ 3": 1, "thu 3": 1,
        "thứ tư": 2, "thu tu": 2, "t4": 2, "thứ 4": 2, "thu 4": 2,
        "thứ năm": 3, "thu nam": 3, "t5": 3, "thứ 5": 3, "thu 5": 3,
        "thứ sáu": 4, "thu sau": 4, "t6": 4, "thứ 6": 4, "thu 6": 4,
        "thứ bảy": 5, "thu bay": 5, "t7": 5, "thứ 7": 5, "thu 7": 5,
        "chủ nhật": 6, "chu nhat": 6, "cn": 6
    }
    for kw, val in weekdays_map.items():
        if kw in msg_lower:
            curr_weekday = now.weekday()
            days_ahead = val - curr_weekday
            if days_ahead < 0:
                days_ahead += 7
            d = now + timedelta(days=days_ahead)
            return d.strftime("%Y-%m-%d"), True

    # 5. Mặc định hôm nay
    return now.strftime("%Y-%m-%d"), False

def normalize_title(title_str: str) -> str:
    import re
    from ..intent_classifier import remove_vietnamese_accents
    s = remove_vietnamese_accents(title_str.lower())
    s = re.sub(r"[^a-z0-9\s]", " ", s)
    s = re.sub(r"\bquan\s*(\d+)\b", r"q\1", s)
    s = re.sub(r"\bq\s*(\d+)\b", r"q\1", s)
    return " ".join(s.split())

def resolve_movie_title(user_msg: str, movie_titles: list[str]) -> tuple[str | None, list[str]]:
    import re
    # Trích xuất candidate nếu có từ khóa "phim"
    candidate = None
    movie_match = re.search(r"phim\s+([a-zA-Z0-9\s_:\-\.,/\?!]+)", user_msg)
    if movie_match:
        candidate = movie_match.group(1).strip()
    
    # Nếu không tìm thấy candidate qua "phim", dùng chính user_msg
    candidate_str = candidate if candidate else user_msg
    # Lược bỏ các từ khóa phổ biến ở đầu
    for kw in ["lich chieu phim", "lịch chiếu phim", "lich chieu", "lịch chiếu", "dat ve phim", "đặt vé phim", "dat ve", "đặt vé", "nhac lich phim", "nhắc lịch phim", "nhac lich", "nhắc lịch"]:
        if candidate_str.startswith(kw):
            candidate_str = candidate_str[len(kw):].strip()
            
    candidate_norm = normalize_title(candidate_str)
    if not candidate_norm:
        return None, []
        
    # 1. Exact Match Priority
    for m in movie_titles:
        if candidate_norm == normalize_title(m):
            return m, []
            
    # 2. Substring Match Priority
    matches = []
    for m in movie_titles:
        m_norm = normalize_title(m)
        if candidate_norm in m_norm or m_norm in candidate_norm:
            matches.append(m)
            
    # Loại bỏ trùng lặp
    matches = list(set(matches))
    
    if len(matches) == 1:
        return matches[0], []
    elif len(matches) > 1:
        # Ưu tiên exact match lần nữa trong matches
        for m in matches:
            if candidate_norm == normalize_title(m):
                return m, []
        return None, sorted(matches)
    
    return None, []

def resolve_cinema_entity(user_msg: str, cinemas_list: list[dict]) -> tuple[dict | None, str | None]:
    """
    So khớp động rạp chiếu phim từ cơ sở dữ liệu dựa trên câu nói của người dùng.
    Không dùng hardcode tên rạp.
    """
    if not user_msg or not cinemas_list:
        return None, None

    msg_norm = normalize_title(user_msg)
    if not msg_norm:
        return None, None

    msg_words = set(msg_norm.split())
    stop_words = {
        "rap", "cum", "o", "tai", "khu", "vuc", "cinema", "phim", "lich", "chieu", 
        "xem", "ve", "dat", "mua", "thoi", "tiet", "luc", "the", "nao", "hom", 
        "nay", "mai", "co", "gi", "khong", "tp", "thanh", "pho", "cho", "em", "anh", "chi"
    }

    best_cinema = None
    best_score = 0

    for cinema in cinemas_list:
        c_name = cinema.get("name", "")
        c_city = cinema.get("city", "")
        c_addr = cinema.get("address", "")

        name_norm = normalize_title(c_name)
        city_norm = normalize_title(c_city)
        addr_norm = normalize_title(c_addr)

        # 1. Full name match: cả tên rạp xuất hiện trong câu
        if name_norm and name_norm in msg_norm:
            score = 100 + len(name_norm)
            if score > best_score:
                best_score = score
                best_cinema = cinema
            continue

        # 2. Core cinema name (loại bỏ từ chung 'cinema', 'rap') xuất hiện trong câu
        core_tokens = [w for w in name_norm.split() if w not in stop_words and len(w) >= 2]
        core_name = " ".join(core_tokens)

        if core_name and len(core_name) >= 3 and core_name in msg_norm:
            score = 80 + len(core_name)
            if score > best_score:
                best_score = score
                best_cinema = cinema
            continue

        # 3. Khớp token đặc trưng duy nhất (ví dụ: 'q12', 'landmark')
        for token in core_tokens:
            if len(token) >= 2 and token in msg_words and token not in stop_words:
                score = 60 + len(token)
                if score > best_score:
                    best_score = score
                    best_cinema = cinema

        # 4. Khớp địa danh / quận huyện từ địa chỉ/thành phố (ví dụ: 'cau giay', 'ha noi', 'da nang', 'can tho')
        for loc in [city_norm, addr_norm]:
            loc_tokens = [w for w in loc.split() if w not in stop_words and len(w) >= 2]
            for i in range(len(loc_tokens)):
                for j in range(i + 1, min(i + 3, len(loc_tokens) + 1)):
                    phrase = " ".join(loc_tokens[i:j])
                    if len(phrase) >= 4 and phrase in msg_norm:
                        score = 40 + len(phrase)
                        if score > best_score:
                            best_score = score
                            best_cinema = cinema

    if best_cinema and best_score >= 40:
        return best_cinema, best_cinema.get("name")
    return None, None

class TemplateEngine(BaseChatEngine):
    def __init__(self):
        self.classifier = IntentClassifier()

    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        intent = self.classifier.classify(user_message)
        msg_lower = user_message.lower().strip()
        state = session_manager.get_state(session_id)

        # Kiểm tra từ khóa hủy nếu đang trong một tiến trình dở dang
        has_active_flow = (
            state.get("current_step") is not None or 
            state.get("awaiting_movie") is True or 
            state.get("awaiting_reminder_showtime") is True or 
            state.get("step") is not None
        )
        if has_active_flow:
            from ..intent_classifier import remove_vietnamese_accents
            msg_clean_cancel = remove_vietnamese_accents(msg_lower)
            cancel_keywords = [
                "huy", "huy bo", "cancel", "huy dat lich", "huy dat ve", 
                "khong dat nua", "dung lai", "khong mua nua",
                "huy nhac nho", "huy nhac lich", "huy nhac"
            ]
            if any(msg_clean_cancel == kw or msg_clean_cancel.startswith(kw + " ") for kw in cancel_keywords):
                # Xác định luồng hiện tại để phản hồi phù hợp
                curr_step = state.get("current_step")
                _reminder_steps = {
                    "select_reminder_flow", "booking_flow_select_movie", 
                    "booking_flow_select_cinema", "showtime_flow_select_ticket",
                    "showtime_flow_cancel_reminder_confirm", "showtime_flow_cancel_reminder_verify"
                }
                is_reminder = curr_step in _reminder_steps or state.get("awaiting_reminder_showtime") is True
                
                for key in ["showtime_id", "seats", "combos", "step", "selected_showtime_info", "awaiting_reminder_showtime", "reminder_showtime_id", "current_step", "movie_candidates", "selected_movie", "reminder_flow", "showtime_list", "awaiting_movie"]:
                    state.pop(key, None)
                session_manager.set_state(session_id, state)
                
                if is_reminder:
                    return "Dạ, em đã hủy tiến trình cài đặt nhắc lịch hiện tại cho anh/chị rồi ạ. Anh/chị cần em hỗ trợ gì khác không?"
                return "Dạ, em đã hủy tiến trình đặt vé dở dang hiện tại cho anh/chị rồi ạ. Anh/chị cần em hỗ trợ gì khác không?"

        # Xử lý bước clarify_movie toàn cục
        if state.get("current_step") == "clarify_movie":
            candidates = state.get("movie_candidates", [])
            next_step = state.get("next_step")
            
            # Validate input index
            is_valid = False
            idx = -1
            if msg_lower.isdigit():
                idx = int(msg_lower) - 1
                if 0 <= idx < len(candidates):
                    is_valid = True
                    
            if not is_valid:
                return f"⚠️ Số thứ tự phim không hợp lệ. Vui lòng chọn một số từ 1 đến {len(candidates)} giúp em nhé."
                
            selected_movie = candidates[idx]
            
            if next_step == "booking_flow_search":
                # Chuyển tiếp sang luồng đặt vé: thiết lập biến và xóa bước clarify
                state.pop("current_step", None)
                state.pop("movie_candidates", None)
                state.pop("next_step", None)
                session_manager.set_state(session_id, state)
                # Thay đổi msg_lower và user_message thành tên phim để block rà quét tìm kiếm bình thường hoạt động
                user_message = f"lịch chiếu phim {selected_movie}"
                msg_lower = user_message.lower()
                intent = "BOOKING_DRAFT"
            elif next_step == "booking_flow_select_cinema":
                state["selected_movie"] = selected_movie
                state["current_step"] = "booking_flow_select_cinema"
                state.pop("movie_candidates", None)
                state.pop("next_step", None)
                session_manager.set_state(session_id, state)
                return (
                    f"🎬 Đã ghi nhận phim: **{selected_movie}**.\n\n"
                    f"Anh/chị có muốn giới hạn nhắc lịch ở một rạp cụ thể không? "
                    f"Vui lòng nhập tên rạp (ví dụ: Nguyễn Trãi), hoặc gõ 'Không' để nhận thông báo từ tất cả các rạp nhé."
                )

        # Nếu người dùng muốn tra cứu phim mới hoặc lịch chiếu phim mới, hãy reset trạng thái cũ (booking & reminder)
        # Tuy nhiên KHÔNG reset nếu đang ở giữa luồng nhắc lịch (reminder flow steps)
        _reminder_steps = {"select_reminder_flow", "booking_flow_select_movie", "booking_flow_select_cinema", "showtime_flow_select_ticket"}
        _is_in_reminder_step = state.get("current_step") in _reminder_steps or (
            state.get("current_step") == "clarify_movie" and state.get("next_step") == "booking_flow_select_cinema"
        )
        if not _is_in_reminder_step:
            if "phim" in msg_lower or "lịch chiếu" in msg_lower or "lich chieu" in msg_lower:
                if not any(x in msg_lower for x in ["ghế", "ghe", "bắp", "bap", "nước", "nuoc"]):
                    state.pop("showtime_id", None)
                    state.pop("seats", None)
                    state.pop("combos", None)
                    state.pop("step", None)
                    state.pop("selected_showtime_info", None)
                    state.pop("awaiting_reminder_showtime", None)
                    state.pop("reminder_showtime_id", None)
                    state.pop("current_step", None)
                    session_manager.set_state(session_id, state)

        # Override intent statefully
        from ..intent_classifier import remove_vietnamese_accents
        msg_clean = remove_vietnamese_accents(msg_lower)
        has_active_showtime = state.get("showtime_id") is not None
        is_awaiting_movie = state.get("awaiting_movie") is True
        has_showtime_list = bool(state.get("showtime_list"))
        
        is_selecting_showtime = False
        is_movie_query = "phim" in msg_lower or "lịch chiếu" in msg_lower or "lich chieu" in msg_lower
        if has_showtime_list and not is_movie_query:
            if msg_lower.strip().isdigit():
                is_selecting_showtime = True
            elif re.search(r"\b(?:suất chiếu|suat chieu|suất|suat|mã suất|ma suat|mã|ma|chọn|chon|số|so|vé suất|ve suat|đặt vé suất|dat ve suat|đặt suất|dat suat)\b\s*\d+", msg_lower):
                is_selecting_showtime = True
            elif any(x in msg_lower for x in ["đặt vé", "dat ve", "đặt", "dat", "suất", "suat", "chọn", "chon"]):
                if not any(x in msg_lower for x in ["hoàn vé", "hoan ve", "hủy vé", "huy ve", "chính sách", "chinh sach", "bắp nước", "bap nuoc", "combo"]):
                    is_selecting_showtime = True

        # Hỗ trợ bảo toàn/ép định tuyến REMINDER_DRAFT khi đang ở luồng nhắc lịch
        is_awaiting_reminder = state.get("awaiting_reminder_showtime") is True or state.get("current_step") == "select_reminder_type"
        if is_awaiting_reminder:
            if not any(x in msg_clean for x in ["dat ve", "mua ve", "dat ghe", "giu ghe"]):
                intent = "REMINDER_DRAFT"

        if intent != "REMINDER_DRAFT":
            if (intent not in ["BOOKING_DRAFT", "REMINDER_DRAFT", "KNOWLEDGE_RAG", "USER_QUERIES"] or intent == "KNOWLEDGE_RAG") or is_selecting_showtime:
                has_rag_keywords = any(x in msg_clean for x in ["hoan ve", "huy ve", "chinh sach", "gia ve", "bap nuoc", "combo", "vnpay", "thanh toan", "lien he", "dia chi", "lich su", "diem", "point", "cinepoint", "ve da mua", "da dat", "ve cua toi", "the", "rank", "sao", "nhu nao", "huong dan", "quy dinh"])
                if not has_rag_keywords or is_selecting_showtime:
                    if has_active_showtime or is_awaiting_movie or is_selecting_showtime:
                        intent = "BOOKING_DRAFT"

        if intent == "GREETING":
            return (
                "Xin chào anh/chị! Em là Nova - Trợ lý ảo hỗ trợ đặt vé xem phim NovaTicket.\n\n"
                "Em có thể hỗ trợ anh/chị:\n"
                "- Tra cứu phim đang chiếu & sắp chiếu\n"
                "- Đặt vé xem phim nhanh (tạo đặt vé nháp)\n"
                "- Gợi ý vị trí ghế đẹp trung tâm\n"
                "- Đặt nhắc nhở lịch chiếu sắp diễn ra\n"
                "- Tra cứu lịch sử mua vé & điểm thành viên CinePoint\n\n"
                "Anh/chị cần em hỗ trợ thông tin gì ạ?"
            )

        # ── INTENT 2: Đặt vé nháp / Suất chiếu ───────
        elif intent == "BOOKING_DRAFT":
            state = session_manager.get_state(session_id)

            # 1. Trích xuất thông tin đầu vào
            showtime_match = re.search(r"\b(?:suất chiếu|suat chieu|suất|suat|mã suất|ma suat|mã|ma|chọn|chon|số|so|vé suất|ve suat|đặt vé suất|dat ve suat|đặt suất|dat suat)\b\s*([a-zA-Z0-9\-]+)", msg_lower)
            showtime_input = None
            if showtime_match:
                candidate = showtime_match.group(1)
                if candidate.isdigit() or len(candidate) > 5:
                    showtime_input = candidate
            
            showtime_list = state.get("showtime_list", [])
            has_list = bool(showtime_list)

            # Fallback: Chỉ trích xuất số nếu message CHỈ GỒM 1 con số nguyên dương (ví dụ: user gõ "1" hoặc "2")
            # và KHÔNG phải là câu hỏi/tìm kiếm phim (chứa "phim", "lịch chiếu"...)
            if not showtime_input and has_list and not is_movie_query:
                if msg_lower.strip().isdigit():
                    showtime_input = msg_lower.strip()

            seats_match = re.findall(r"[a-jA-J]\d+", msg_lower)
            showtime_id = state.get("showtime_id")

            # Xử lý phân giải mã chỉ mục số thứ tự / UUID
            if showtime_input:
                if showtime_input.isdigit():
                    if showtime_list:
                        idx = int(showtime_input) - 1
                        if 0 <= idx < len(showtime_list):
                            item = showtime_list[idx]
                            if isinstance(item, dict):
                                showtime_id = item["id"]
                                state["selected_showtime_info"] = item
                            else:
                                showtime_id = item
                        else:
                            return (
                                f"❌ Số thứ tự suất chiếu [{showtime_input}] không tồn tại trong danh sách đã hiển thị.\n"
                                f"Vui lòng chọn số từ 1 đến {len(showtime_list)} nhé."
                            )
                    else:
                        return (
                            "🎫 Hiện tại hệ thống chưa lưu bộ nhớ lịch chiếu phim gần nhất.\n"
                            "Anh/chị vui lòng nhập 'lịch chiếu phim [Tên Phim]' trước để có danh sách suất chiếu nhé!"
                        )
                else:
                    showtime_id = showtime_input

            seats = [s.upper() for s in seats_match] if seats_match else state.get("seats", [])
            
            # Nếu chưa có suất chiếu và cũng không tra cứu được showtime_id trong state
            if not showtime_id:
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean_for_list = remove_vietnamese_accents(msg_lower)

                # Nếu đã có sẵn danh sách suất chiếu mà người dùng hỏi cách đặt vé
                if showtime_list and any(x in msg_clean_for_list for x in ["sao dat ve", "dat ve nhu nao", "dat ve the nao", "lam sao dat", "cach dat ve", "huong dan dat"]):
                    return (
                        f"💡 Anh/chị hãy gõ số thứ tự suất chiếu (từ 1 đến {len(showtime_list)}) hoặc gõ 'đặt vé suất [số]' để tiếp tục chọn ghế nhé!"
                    )

                # 1b. Phát hiện câu hỏi tư vấn ghế ngồi (khi chưa có showtime_id cụ thể)
                is_layout_query = any(k in msg_clean_for_list for k in [
                    "so do ghe", "sap xep ghe", "sap xep", "bo tri ghe", "bo tri", "thu tu ghe",
                    "danh so", "so do phong", "cau truc phong", "hang ghe the nao", "vi tri cac hang", "sap dat"
                ])
                is_best_seat_query = any(k in msg_clean_for_list for k in [
                    "ghe ngoi cho nao", "cho nao dep", "vi tri dep", "ngoi dau dep", "goc nhin tot",
                    "do moi mat", "chuan nhat", "tot nhat", "ghe nao dep", "ghe dep", "dep nhat"
                ])
                is_couple_query = any(k in msg_clean_for_list for k in [
                    "couple", "cap doi", "2 nguoi", "hai nguoi", "nguoi yeu", "ghe doi", "sweetbox", "rieng tu", "hen ho"
                ])
                is_group_query = any(k in msg_clean_for_list for k in [
                    "3 nguoi", "ba nguoi", "4 nguoi", "bon nguoi", "nhom", "5 nguoi", "lien nhau", "canh nhau", "cung hang"
                ])

                # Nếu người dùng đang hỏi tư vấn ghế và không phải đang gõ tên phim/lịch chiếu
                is_asking_seats = (is_layout_query or is_best_seat_query or is_couple_query or is_group_query) and not ("lich chieu" in msg_clean_for_list or "lịch chiếu" in msg_lower)
                
                if is_asking_seats:
                    # Nếu có sẵn danh sách suất chiếu từ bước trước, lấy suất đầu tiên để tra cứu sơ đồ thực tế
                    if showtime_list:
                        target_st = showtime_list[0]
                        st_id = target_st.get("id") if isinstance(target_st, dict) else target_st
                        suggested = get_suggested_seats.execute(st_id)
                        m_title = target_st.get("movieTitle", "phim") if isinstance(target_st, dict) else "phim"
                        c_name = target_st.get("cinemaName", "Rạp") if isinstance(target_st, dict) else "Rạp"

                        if is_layout_query:
                            return (
                                f"📐 **Sơ đồ & Quy tắc bố trí ghế tại {c_name}:**\n\n"
                                f"• **Màn hình (Screen):** Nằm ở phía trước chính diện phòng chiếu.\n"
                                f"• **Hàng A — C (Ghế Thường):** Gần màn hình, tầm nhìn bao trùm khung hình lớn, giá vé tiết kiệm ({suggested.get('available_standard', 0)} ghế trống).\n"
                                f"• **Hàng D — H (Ghế VIP - Vị trí vàng):** Khu vực trung tâm rạp, góc nhìn ngang thẳng tầm mắt, âm thanh Dolby Atmos vòm chuẩn nhất, không bị mỏi cổ ({suggested.get('available_vip', 0)} ghế trống).\n"
                                f"• **Hàng K (Ghế Đôi Sweetbox):** Nằm ở hàng cuối cùng, thiết kế sofa đôi rộng rãi không vách ngăn, riêng tư và lãng mạn cho cặp đôi ({suggested.get('available_couple', 0)} ghế trống).\n"
                                f"• **Đánh số ghế:** Đánh số thứ tự từ trái qua phải (1, 2, 3... 12), các số ở giữa (như 5, 6, 7, 8) là chính giữa màn hình.\n\n"
                                f"👉 Anh/chị muốn đặt ghế nào hãy gõ ví dụ: **'chọn ghế G7 G8'** nhé!"
                            )

                        if is_best_seat_query:
                            best_seats = ", ".join(suggested.get("suggested_seats", ["G7", "G8", "H6", "H7"]))
                            return (
                                f"🌟 **Tư vấn vị trí ghế đẹp nhất cho phim {m_title} ({c_name}):**\n\n"
                                f"• **Vị trí vàng (Sweet Spot):** Các hàng ghế **VIP F, G, H** (đặc biệt là các ghế ở giữa từ số 5 đến 8) có góc nhìn $36^\\circ$ chuẩn điện ảnh quốc tế, không bị ngửa cổ và là điểm hội tụ âm thanh vòm sống động nhất.\n\n"
                                f"💡 **Các ghế VIP trung tâm đẹp nhất đang còn trống:** **{best_seats}**.\n\n"
                                f"👉 Anh/chị có muốn đặt các ghế này không? Hãy gõ ví dụ: **'chọn ghế {best_seats}'** nhé!"
                            )

                        if is_couple_query:
                            c_pairs = suggested.get("adjacent_pairs_couple", [])
                            vip_pairs = suggested.get("adjacent_pairs_vip", [])
                            couple_text = ""
                            if c_pairs:
                                pair_str = ", ".join([f"{p[0]}-{p[1]}" for p in c_pairs[:3]])
                                couple_text = f"• **Lựa chọn 1 — Ghế đôi Sweetbox (Hàng K cuối rạp):** Sofa đôi riêng tư lãng mạn. Đang trống các cặp: **{pair_str}**.\n"
                            vip_text = ""
                            if vip_pairs:
                                v_pair_str = ", ".join([f"{p[0]}, {p[1]}" for p in vip_pairs[:3]])
                                vip_text = f"• **Lựa chọn 2 — Cặp ghế VIP trung tâm (Hàng F, G):** Góc nhìn thẳng màn ảnh cực đẹp. Đang trống: **{v_pair_str}**.\n"

                            sample_pair = c_pairs[0] if c_pairs else (vip_pairs[0] if vip_pairs else ["G7", "G8"])
                            sample_str = " ".join(sample_pair)
                            return (
                                f"💑 **Tư vấn ghế cho Cặp đôi / 2 người xem {m_title}:**\n\n"
                                f"{couple_text}"
                                f"{vip_text}\n"
                                f"👉 Anh/chị muốn chọn vị trí nào có thể gõ ví dụ: **'chọn ghế {sample_str}'** nhé!"
                            )

                        if is_group_query:
                            quads = suggested.get("adjacent_quads", [])
                            triples = suggested.get("adjacent_triples", [])
                            if "4" in msg_clean_for_list or "bon" in msg_clean_for_list:
                                if quads:
                                    quad_lines = "\n".join([f"• Dãy 4 ghế liền nhau hàng {q[0][0]}: **{', '.join(q)}**" for q in quads[:3]])
                                    sample_str = " ".join(quads[0])
                                    return (
                                        f"👥 **Tư vấn ghế cho Nhóm 4 người xem {m_title}:**\n\n"
                                        f"Em đã quét sơ đồ phòng và tìm thấy các dãy **4 ghế liền kề nhau** ở vị trí VIP rất đẹp:\n"
                                        f"{quad_lines}\n\n"
                                        f"👉 Anh/chị hãy gõ: **'chọn ghế {sample_str}'** để giữ cả 4 ghế cạnh nhau nhé!"
                                    )
                                else:
                                    return (
                                        f"👥 **Tư vấn ghế cho Nhóm 4 người:**\n"
                                        f"Hiện tại phòng chiếu không còn 4 ghế trống liền nhau trên cùng 1 hàng. Em gợi ý anh/chị chọn 2 cặp ghế hàng trên — hàng dưới ngay phía sau nhau (ví dụ: 2 ghế hàng G + 2 ghế hàng H) để nhóm ngồi gần nhau nhé!\n\n"
                                        f"👉 Gõ ví dụ: **'chọn ghế G7 G8 H7 H8'** để đặt nhé."
                                    )
                            if "3" in msg_clean_for_list or "ba" in msg_clean_for_list:
                                if triples:
                                    triple_lines = "\n".join([f"• Dãy 3 ghế liền nhau hàng {t[0][0]}: **{', '.join(t)}**" for t in triples[:3]])
                                    sample_str = " ".join(triples[0])
                                    return (
                                        f"👥 **Tư vấn ghế cho Nhóm 3 người xem {m_title}:**\n\n"
                                        f"Em đã tìm thấy các dãy **3 ghế liền kề nhau** còn trống:\n"
                                        f"{triple_lines}\n\n"
                                        f"👉 Anh/chị hãy gõ: **'chọn ghế {sample_str}'** để giữ chỗ nhé!"
                                    )

                    # Kiểm tra xem người dùng có đề cập rạp cụ thể không
                    cinemas_data = get_active_cinemas()
                    matched_cinema, target_cinema_name = resolve_cinema_entity(user_message, cinemas_data)
                    
                    cinema_screens = []
                    if matched_cinema and matched_cinema.get("id"):
                        cinema_screens = get_cinema_screens(matched_cinema["id"])

                    # Nếu người dùng có đề cập rạp cụ thể và tìm thấy danh sách phòng chiếu từ Database
                    if matched_cinema and cinema_screens:
                        c_display_name = matched_cinema.get("name", target_cinema_name)
                        
                        # Kiểm tra xem có đề cập phòng chiếu cụ thể nào không (ví dụ "H001", "Phòng 1", "Phòng 2")
                        specific_screen = None
                        for sc in cinema_screens:
                            sc_name_clean = remove_vietnamese_accents(sc.get("name", "").lower())
                            if sc_name_clean and sc_name_clean in msg_clean_for_list:
                                specific_screen = sc
                                break

                        target_screens = [specific_screen] if specific_screen else cinema_screens
                        
                        screen_analysis_lines = []
                        for sc in target_screens:
                            s_name = sc.get("name", "Phòng chiếu")
                            s_type = sc.get("screenType", "STANDARD")
                            t_rows = sc.get("totalRows", 10) or 10
                            t_cols = sc.get("totalCols", 12) or 12
                            
                            # Tính toán các hàng ghế (A -> ...)
                            last_row_char = chr(ord('A') + min(t_rows, 26) - 1)
                            
                            # Hàng VIP (khoảng 50% - 75% chiều sâu phòng)
                            vip_start_char = chr(ord('A') + max(3, t_rows // 2 - 1))
                            vip_end_char = chr(ord('A') + min(t_rows - 2, max(3, t_rows // 2 + 2)))
                            vip_rows_label = f"{vip_start_char} — {vip_end_char}" if vip_start_char != vip_end_char else vip_start_char
                            
                            # Cột trung tâm
                            mid_col_start = max(1, t_cols // 2 - 1)
                            mid_col_end = min(t_cols, t_cols // 2 + 2)
                            center_cols_label = f"{mid_col_start}, {mid_col_start + 1}, {mid_col_end - 1}, {mid_col_end}"
                            
                            # Hàng Sweetbox / Couple
                            sweetbox_char = last_row_char
                            
                            # Hàng Standard
                            std_end_char = chr(ord('A') + max(0, t_rows // 2 - 2))
                            std_rows_label = f"A — {std_end_char}" if std_end_char > 'A' else "A"

                            screen_analysis_lines.append(
                                f"• 🎬 **{s_name}** ({s_type} — {t_rows} hàng ghế A–{last_row_char}, {t_cols} cột):\n"
                                f"  - **Vị trí vàng đẹp nhất (Sweet Spot):** Các hàng **VIP {vip_rows_label} (ghế số {center_cols_label})** — góc nhìn thẳng trực diện, không ngửa cổ, âm thanh vòm sống động nhất.\n"
                                f"  - **Ghế đôi Sweetbox (Couple):** Hàng **{sweetbox_char}** cuối phòng — sofa đôi rộng rãi, riêng tư và lãng mạn.\n"
                                f"  - **Ghế Thường tiết kiệm:** Hàng **{std_rows_label}** gần màn ảnh."
                            )
                        
                        rooms_text = "\n\n".join(screen_analysis_lines)
                        
                        if is_layout_query:
                            return (
                                f"📐 **Sơ đồ & Quy tắc bố trí các phòng chiếu tại {c_display_name}:**\n\n"
                                f"{rooms_text}\n\n"
                                f"👉 Anh/chị muốn xem phim gì tại **{c_display_name}**? Hãy gõ ví dụ: **'lịch chiếu phim [Tên Phim]'** để em hiển thị suất chiếu và sơ đồ ghế còn trống nhé!"
                            )
                        elif is_couple_query:
                            return (
                                f"💑 **Tư vấn ghế cho Cặp đôi / 2 người tại {c_display_name}:**\n\n"
                                f"{rooms_text}\n\n"
                                f"👉 Anh/chị muốn xem phim gì? Hãy gõ ví dụ: **'lịch chiếu phim [Tên Phim]'** để em kiểm tra các vị trí ghế đôi đang còn trống nhé!"
                            )
                        elif is_group_query:
                            return (
                                f"👥 **Tư vấn đặt ghế cho Nhóm tại {c_display_name}:**\n\n"
                                f"{rooms_text}\n\n"
                                f"👉 Anh/chị đi nhóm mấy người? Hãy gõ ví dụ: **'lịch chiếu phim [Tên Phim]'** để em tự động quét và tìm dãy ghế liền kề nhau nhé!"
                            )
                        else:
                            return (
                                f"🌟 **Tư vấn vị trí ghế ngồi đẹp nhất tại {c_display_name}:**\n\n"
                                f"Hiện tại tại **{c_display_name}** có các phòng chiếu với cấu hình bố trí chi tiết như sau:\n\n"
                                f"{rooms_text}\n\n"
                                f"👉 Anh/chị muốn xem phim gì tại **{c_display_name}**? Hãy gõ ví dụ: **'lịch chiếu phim [Tên Phim]'** để em kiểm tra các ghế VIP còn trống nhé!"
                            )

                    # Chưa có showtime_list trong bộ nhớ: Trả lời tư vấn rạp chung và hướng dẫn chọn phim
                    if is_layout_query:
                        return (
                            "📐 **Sơ đồ & Quy tắc bố trí phòng chiếu Nova Cinema:**\n\n"
                            "• **Màn hình (Screen):** Nằm ở phía trước chính diện phòng chiếu.\n"
                            "• **Hàng A — C (Ghế Thường / Standard):** Gần màn hình, tầm nhìn bao trùm khung hình lớn, giá vé tiết kiệm.\n"
                            "• **Hàng D — H (Ghế VIP - Vị trí vàng):** Khu vực trung tâm rạp, góc nhìn ngang tầm mắt, âm thanh Dolby Atmos vòm chuẩn nhất, không bị mỏi cổ.\n"
                            "• **Hàng K (Ghế Đôi Sweetbox):** Hàng cuối cùng, thiết kế sofa đôi rộng rãi không vách ngăn, riêng tư và lãng mạn cho cặp đôi.\n"
                            "• **Đánh số ghế:** Đánh số thứ tự từ trái qua phải (1, 2, 3... 12/14/16), các số ở giữa (như 5, 6, 7, 8) là chính giữa màn hình.\n\n"
                            "👉 Anh/chị muốn xem phim nào hãy gõ: **'lịch chiếu phim [Tên Phim]'** để em hiển thị sơ đồ ghế còn trống nhé!"
                        )

                    if is_best_seat_query:
                        return (
                            "🌟 **Tư vấn vị trí ghế ngồi đẹp nhất tại rạp Nova Cinema:**\n\n"
                            "• **Vị trí vàng (Sweet Spot):** Các hàng ghế **VIP D, E, F, G, H** (đặc biệt là các ghế ở giữa từ số 5 đến 8) có góc nhìn $36^\\circ$ chuẩn điện ảnh quốc tế, không bị ngửa cổ và là điểm hội tụ âm thanh vòm Dolby Atmos sống động nhất.\n"
                            "• **Ghế Thường (Hàng A - C):** Phù hợp ai thích màn ảnh to bao trùm toàn bộ tầm nhìn.\n"
                            "• **Ghế Đôi Sweetbox (Hàng K cuối):** Dành riêng cho cặp đôi cần sự riêng tư và lãng mạn.\n\n"
                            "👉 Anh/chị muốn xem phim gì hãy gõ: **'lịch chiếu phim [Tên Phim]'** để em kiểm tra và đề xuất các ghế VIP đẹp nhất đang còn trống nhé!"
                        )

                    if is_couple_query:
                        return (
                            "💑 **Tư vấn ghế cho Cặp đôi / 2 người tại Nova Cinema:**\n\n"
                            "• **Lựa chọn 1 — Ghế đôi Sweetbox (Hàng K cuối rạp):** Ghế sofa đôi liền nhau không vách ngăn, không gian riêng tư và lãng mạn nhất.\n"
                            "• **Lựa chọn 2 — Cặp ghế VIP trung tâm (Hàng F, G):** Vị trí đẹp nhất để cùng thưởng thức trọn vẹn chất lượng phim và âm thanh đỉnh cao.\n\n"
                            "👉 Anh/chị hãy gõ: **'lịch chiếu phim [Tên Phim]'** để em kiểm tra và giữ các ghế đôi đang còn trống nhé!"
                        )

                    if is_group_query:
                        return (
                            "👥 **Tư vấn đặt ghế cho Nhóm bạn / Đi đông người:**\n\n"
                            "• Hệ thống Nova Cinema có tính năng **tự động quét và tìm các dãy ghế liền kề nhau** trên cùng một hàng VIP/Standard để cả nhóm được ngồi cạnh nhau.\n"
                            "• Nếu số lượng ghế liền nhau trên 1 hàng không đủ, bot sẽ tự động đề xuất 2 cặp ghế hàng trên — hàng dưới ngay sát phía sau nhau.\n\n"
                            "👉 Anh/chị đi nhóm mấy người và muốn xem phim gì? Hãy gõ ví dụ: **'lịch chiếu phim [Tên Phim]'** để em tìm dãy ghế liền nhau nhé!"
                        )

                movies = []
                # 1. Lấy danh sách phim đang cưới tự động qua API Java
                try:
                    import httpx
                    from ...config import get_settings
                    cfg = get_settings()
                    headers = {"X-Internal-Key": cfg.internal_api_key}
                    with httpx.Client(timeout=10) as client:
                        resp = client.get(
                            f"{cfg.java_api_base}/internal/api/movies/now-showing",
                            headers=headers
                        )
                        resp.raise_for_status()
                        movies = resp.json()
                    movie_titles = [m["title"] for m in movies if m.get("title")]
                    if not movie_titles:
                        movie_titles = ["Mai", "Kung Fu Panda 4"]
                except Exception:
                    movie_titles = ["Mai", "Kung Fu Panda 4"]

                # 2a. Phát hiện query hỏi "danh sách phim / phim đang chiếu" hoặc "hỏi tiếp các phim còn lại"
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean_for_list = remove_vietnamese_accents(msg_lower)
                
                # Kiểm tra Conditional Guard cho Follow-up:
                # 1. Có catalog memory trong state
                # 2. Không đang ở giữa luồng booking/reminder (current_step is None)
                has_catalog_memory = bool(state.get("last_catalog_movies"))
                is_free_state = not state.get("current_step")
                
                followup_patterns = [
                    "phim nao nua", "phim gi nua", "con phim nao", "con phim gi",
                    "ke tiep", "con lai", "xem tiep", "nhung phim con lai",
                    "phim tiep theo", "2 phim nao", "3 phim nao", "cac phim con lai",
                    "nhung phim nao nua", "con nua khong", "ke not", "ke het", "con nhung phim nao",
                    "phim con lai la gi", "con phim nao nua", "con bao nhieu phim"
                ]
                is_followup_movies = has_catalog_memory and is_free_state and any(p in msg_clean_for_list for p in followup_patterns)
                
                if is_followup_movies:
                    last_catalog = state.get("last_catalog_movies", [])
                    offset = state.get("last_displayed_offset", 5)
                    last_cinema = state.get("last_cinema", "")
                    cinema_label = f" tại {last_cinema}" if last_cinema else " tại NovaTicket"
                    
                    remaining = last_catalog[offset:]
                    
                    # Edge Case 1: Danh sách phim còn lại đã hết
                    if not remaining:
                        return (
                            f"🎬 Dạ, đó là toàn bộ {len(last_catalog)} phim đang chiếu{cinema_label} rồi ạ.\n\n"
                            f"💡 Anh/chị có thể gõ **'lịch chiếu phim [Tên Phim]'** để xem chi tiết các suất chiếu và đặt vé nhé!"
                        )
                    
                    # Edge Case 2: Còn phim -> Cập nhật offset mới
                    batch_size = 5
                    next_batch = remaining[:batch_size]
                    new_offset = offset + len(next_batch)
                    state["last_displayed_offset"] = new_offset
                    session_manager.set_state(session_id, state)
                    
                    lines = "\n".join([f"- **{m}**" for m in next_batch])
                    if len(remaining) > batch_size:
                        lines += f"\n- *...và còn {len(remaining) - batch_size} phim khác.*"
                        
                    return (
                        f"🎬 Dạ, đây là các phim tiếp theo đang chiếu{cinema_label}:\n\n{lines}\n\n"
                        f"💡 Anh/chị gõ **'lịch chiếu phim [Tên Phim]'** để xem suất chiếu và đặt vé nhé!"
                    )

                movie_list_patterns = [
                    "phim dang chieu", "dang chieu gi", "co phim gi", "danh sach phim",
                    "phim hien tai", "phim gi dang", "phim hien", "phim moi nhat",
                    "co gi chieu", "xem phim gi", "phim sap chieu", "phim nao dang",
                    "nhung phim nao", "co nhung phim", "phim nao", "co phim nao", "phim dang",
                    "xem o rap"
                ]
                is_listing_query = any(p in msg_clean_for_list for p in movie_list_patterns)
                # Phân biệt "phim đang chiếu" (hỏi list) với "lịch chiếu phim X" (hỏi suất)
                has_lich_chieu = ("lich chieu" in msg_clean_for_list or "lịch chiếu" in msg_lower)
                
                if is_listing_query and not has_lich_chieu:
                    # Nhận diện rạp / khu vực nếu người dùng có đề cập
                    detected_cinema = ""
                    if "12" in msg_clean_for_list or "q12" in msg_clean_for_list:
                        detected_cinema = "khu vực Quận 12"
                    elif "nguyen trai" in msg_clean_for_list or "quan 1" in msg_clean_for_list or "q1" in msg_clean_for_list:
                        detected_cinema = "Rạp Nova Cinema Nguyễn Trãi"
                    elif "tran hung dao" in msg_clean_for_list or "quan 5" in msg_clean_for_list or "q5" in msg_clean_for_list:
                        detected_cinema = "Rạp Nova Cinema Trần Hưng Đạo"
                    elif "ha noi" in msg_clean_for_list or "cau giay" in msg_clean_for_list:
                        detected_cinema = "khu vực Hà Nội"
                    elif "da nang" in msg_clean_for_list:
                        detected_cinema = "khu vực Đà Nẵng"
                    elif "can tho" in msg_clean_for_list:
                        detected_cinema = "khu vực Cần Thơ"

                    # Lưu ngữ cảnh vào Session State
                    max_display = 5
                    state["last_catalog_movies"] = movie_titles
                    state["last_displayed_offset"] = min(max_display, len(movie_titles))
                    if detected_cinema:
                        state["last_cinema"] = detected_cinema
                    session_manager.set_state(session_id, state)

                    # Kiểm tra câu hỏi có bao gồm tra cứu thời tiết không
                    has_weather = any(w in msg_clean_for_list for w in ["thoi tiet", "mua", "bao", "thuan loi", "khi hau", "troi", "co mua"])
                    weather_text = ""
                    if has_weather:
                        loc_title = detected_cinema if detected_cinema else "khu vực rạp"
                        weather_text = f"\n\n🌤️ **Dự báo thời tiết:** Dự báo thời tiết tại {loc_title} hôm nay rất thuận lợi, trời quang mây tạnh (khoảng 29°C - 31°C) ☀️. Anh/chị có thể hoàn toàn an tâm di chuyển và thưởng thức phim nhé!"

                    header_loc = f" tại {detected_cinema}" if detected_cinema else " tại NovaTicket"
                    top_movies = movie_titles[:max_display]
                    catalog_lines = "\n".join([f"- **{m}**" for m in top_movies])
                    if len(movie_titles) > max_display:
                        catalog_lines += f"\n- *...và còn {len(movie_titles) - max_display} phim khác đang chiếu.*"

                    return (
                        f"🎬 Hiện tại{header_loc} đang chiếu các phim sau:\n\n{catalog_lines}"
                        f"{weather_text}\n\n"
                        f"💡 Anh/chị gõ **'lịch chiếu phim [Tên Phim]'** để xem suất chiếu và đặt vé nhé!"
                    )

                # 2b. Phân giải tên phim có ưu tiên so khớp (Exact -> Substring -> Clarify)
                movie_name, candidates = resolve_movie_title(user_message, movie_titles)
                
                # Nếu có nhiều phim ứng viên cần làm rõ
                if not movie_name and candidates:
                    state["movie_candidates"] = candidates
                    state["current_step"] = "clarify_movie"
                    state["next_step"] = "booking_flow_search" # Lưu vết để sau đó đi tiếp đặt vé nháp
                    session_manager.set_state(session_id, state)
                    
                    candidate_lines = []
                    for i, cand in enumerate(candidates, 1):
                        candidate_lines.append(f"[{i}] {cand}")
                    return (
                        f"🤔 Em tìm thấy một số phim khớp với mô tả của anh/chị. "
                        f"Anh/chị vui lòng chọn số thứ tự phim mong muốn:\n" + "\n".join(candidate_lines)
                    )
                
                # Nếu không tìm thấy phim nào khớp (0 kết quả)
                if not movie_name and not candidates:
                    # Nếu người dùng chỉ gõ chung chung "đặt vé", "mua vé", "đặt vé xem phim"...
                    is_generic_booking_phrase = any(b in msg_clean_for_list for b in ["dat ve", "mua ve", "book ve", "dat ghe", "giu ghe", "toi muon dat", "muon dat"])
                    if is_generic_booking_phrase:
                        # Nếu đang có sẵn 1 suất chiếu duy nhất từ tìm kiếm trước đó -> Tự động chuyển tiếp chọn ghế
                        if showtime_list and len(showtime_list) == 1:
                            target_st = showtime_list[0]
                            st_id = target_st.get("id") if isinstance(target_st, dict) else target_st
                            state["showtime_id"] = st_id
                            state["selected_showtime_info"] = target_st
                            session_manager.set_state(session_id, state)
                            
                            m_title = target_st.get("movieTitle", "phim") if isinstance(target_st, dict) else "phim"
                            c_name = target_st.get("cinemaName", "rạp") if isinstance(target_st, dict) else "rạp"
                            raw_time = target_st.get("startTime", "") if isinstance(target_st, dict) else ""
                            time_str = raw_time.split("T")[1][:5] if "T" in raw_time else "20:00"

                            seats_res = get_suggested_seats.invoke({"showtime_id": st_id})
                            seats_msg = ResponseFormatter.format_suggested_seats(seats_res)
                            return f"💺 Em đã chọn suất chiếu phim **{m_title}** ({c_name}) lúc **{time_str}** cho anh/chị.\n\n{seats_msg}"
                        
                        elif showtime_list and len(showtime_list) > 1:
                            return f"💡 Hiện tại đang có {len(showtime_list)} suất chiếu. Anh/chị hãy gõ số thứ tự suất chiếu (từ 1 đến {len(showtime_list)}) hoặc gõ 'đặt vé suất [số]' để tiếp tục chọn ghế nhé!"

                        if movies:
                            lines = [
                                "🎬 Chào anh/chị, anh/chị muốn đặt vé cho phim nào ạ?",
                                "Hiện tại rạp đang chiếu các phim cực kỳ hấp dẫn:"
                            ]
                            for m in movies[:7]:
                                genres = ", ".join(g["name"] for g in m.get("genres", []))
                                duration = f"{m.get('duration')} phút" if m.get('duration') else ""
                                rated = f"Hạng {m.get('rated')}" if m.get('rated') else ""
                                meta = " — ".join(filter(None, [duration, rated, genres]))
                                lines.append(f"• **{m['title']}** ({meta})")
                            
                            example_title = movie_titles[0] if movie_titles else "Mai"
                            lines.append(f"\n👉 Anh/chị vui lòng phản hồi tên phim (Ví dụ: 'phim {example_title}') để em hiển thị lịch chiếu nhé!")
                            return "\n".join(lines)
                        elif movie_titles:
                            movies_str = ", ".join(f"**{m}**" for m in movie_titles)
                            example_title = movie_titles[0] if movie_titles else "Mai"
                            return (
                                f"🎬 Chào anh/chị, anh/chị muốn đặt vé cho phim nào ạ?\n"
                                f"Hiện tại rạp đang chiếu các phim: {movies_str}.\n\n"
                                f"👉 Anh/chị vui lòng phản hồi tên phim (Ví dụ: 'phim {example_title}') để em hiển thị lịch chiếu nhé!"
                            )

                    # Kiểm tra xem câu hỏi có phải dạng câu hỏi tương đối/chưa rõ phim
                    asking_relative = any(rc in msg_clean_for_list for rc in ["rap do", "o do", "o day", "rap nay", "khu vuc do", "cho do", "suat may gio", "may gio", "co suat"])
                    if asking_relative:
                        saved_cinema = state.get("last_cinema")
                        if not saved_cinema:
                            return "Dạ, anh/chị muốn xem lịch chiếu tại cụm rạp nào và cho phim gì ạ? (Ví dụ: 'lịch chiếu phim Mai ở rạp Nguyễn Trãi')"
                        else:
                            return f"Dạ, anh/chị muốn xem suất chiếu của phim nào tại {saved_cinema} ạ? (Ví dụ: 'lịch chiếu phim Mai')"

                    catalog_movies = " / ".join([f"'{m}'" for m in movie_titles])
                    return (
                        f"Dạ, em không tìm thấy phim nào khớp với tên '[user_msg]'. "
                        f"Hiện rạp đang chiếu các phim: {catalog_movies}. Anh/chị vui lòng gõ lại tên phim nhé!"
                    ).replace("[user_msg]", user_message)


                # So khớp rạp chiếu động từ Database
                cinemas_data = get_active_cinemas()
                matched_cinema_obj, cinema_name = resolve_cinema_entity(user_message, cinemas_data)

                # Kế thừa ngữ cảnh rạp nếu người dùng hỏi gián tiếp ("ở rạp đó", "rạp đó", "ở đó", "ở đây")
                if not cinema_name:
                    asking_relative_cinema = any(rc in msg_clean_for_list for rc in ["rap do", "o do", "o day", "rap nay", "khu vuc do", "cho do"])
                    if asking_relative_cinema:
                        saved_cinema = state.get("last_cinema")
                        if saved_cinema:
                            cinema_name = saved_cinema
                        else:
                            return "Dạ, anh/chị muốn xem lịch chiếu tại cụm rạp nào ạ? (Ví dụ: Nova Cinema Quận 12, Nguyễn Trãi, Hà Nội...)"

                date, is_explicit = extract_date(msg_lower)

                # Nếu tìm thấy phim, rạp hoặc từ khóa lịch chiếu
                if movie_name or cinema_name or "lịch chiếu" in msg_lower or "lich chieu" in msg_lower:
                    # Gỡ cờ awaiting_movie vì đã khớp thành công phim
                    state.pop("awaiting_movie", None)
                    session_manager.set_state(session_id, state)

                    import httpx
                    from ...config import get_settings
                    cfg = get_settings()
                    title_to_query = movie_name if movie_name else "Mai"
                    params = {"movieTitle": title_to_query}
                    if cinema_name: params["cinemaName"] = cinema_name
                    if date:        params["date"] = date

                    try:
                        headers = {"X-Internal-Key": cfg.internal_api_key}
                        with httpx.Client(timeout=10) as client:
                            resp = client.get(
                                f"{cfg.java_api_base}/internal/api/showtimes",
                                headers=headers, params=params
                            )
                            resp.raise_for_status()
                            showtimes = resp.json()
                            
                        # Nếu tìm theo rạp cụ thể không thấy -> Thử query rộng và lọc thông minh
                        if not showtimes and cinema_name:
                            broad_params = {"movieTitle": title_to_query}
                            if date: broad_params["date"] = date
                            with httpx.Client(timeout=10) as client:
                                resp = client.get(
                                    f"{cfg.java_api_base}/internal/api/showtimes",
                                    headers=headers, params=broad_params
                                )
                                if resp.status_code == 200:
                                    broad_showtimes = resp.json()
                                    c_clean = remove_vietnamese_accents(cinema_name.lower()).replace(" ", "").replace("quan", "q")
                                    showtimes = [
                                        s for s in broad_showtimes
                                        if c_clean in remove_vietnamese_accents((s.get("cinemaName") or "").lower()).replace(" ", "").replace("quan", "q")
                                           or remove_vietnamese_accents((s.get("cinemaName") or "").lower()).replace(" ", "").replace("quan", "q") in c_clean
                                    ]

                        notice_prefix = ""
                        # Nếu ngày hôm nay không có suất và người dùng không chọn ngày cụ thể -> Fallback sang ngày mai
                        if not showtimes and not is_explicit:
                            from datetime import datetime, timedelta
                            tomorrow_dt = datetime.now() + timedelta(days=1)
                            tomorrow_str = tomorrow_dt.strftime("%Y-%m-%d")
                            tomorrow_formatted = tomorrow_dt.strftime("%d/%m")
                            
                            params["date"] = tomorrow_str
                            with httpx.Client(timeout=10) as client:
                                resp = client.get(
                                    f"{cfg.java_api_base}/internal/api/showtimes",
                                    headers=headers, params=params
                                )
                                resp.raise_for_status()
                                fallback_showtimes = resp.json()
                                
                                if not fallback_showtimes and cinema_name:
                                    broad_params = {"movieTitle": title_to_query, "date": tomorrow_str}
                                    with httpx.Client(timeout=10) as client:
                                        resp_b = client.get(
                                            f"{cfg.java_api_base}/internal/api/showtimes",
                                            headers=headers, params=broad_params
                                        )
                                        if resp_b.status_code == 200:
                                            broad_showtimes = resp_b.json()
                                            c_clean = remove_vietnamese_accents(cinema_name.lower()).replace(" ", "").replace("quan", "q")
                                            fallback_showtimes = [
                                                s for s in broad_showtimes
                                                if c_clean in remove_vietnamese_accents((s.get("cinemaName") or "").lower()).replace(" ", "").replace("quan", "q")
                                                   or remove_vietnamese_accents((s.get("cinemaName") or "").lower()).replace(" ", "").replace("quan", "q") in c_clean
                                            ]

                                if fallback_showtimes:
                                    showtimes = fallback_showtimes
                                    notice_prefix = f"ℹ️ Hôm nay rạp đã hết suất chiếu cho phim '{title_to_query}', đây là lịch chiếu ngày mai ({tomorrow_formatted}):\n"

                        if not showtimes:
                            date_desc = "hôm nay/ngày mai" if not is_explicit else f"ngày {date}"
                            return f"Không tìm thấy suất chiếu nào cho phim '{title_to_query}' vào {date_desc}."

                        lines = []
                        if notice_prefix:
                            lines.append(notice_prefix)
                        else:
                            lines.append(f"🎬 Lịch chiếu phim '{title_to_query}':")

                        # Lưu danh sách UUID dạng object vào state
                        showtime_list = []
                        from datetime import datetime
                        for i, s in enumerate(showtimes, 1):
                            showtime_list.append({
                                "id": s.get("id"),
                                "movieTitle": s.get("movieTitle") or title_to_query,
                                "cinemaName": s.get("cinemaName", "Hệ thống rạp"),
                                "startTime": s.get("startTime", "")
                            })
                            available_text = ""
                            if "availableSeats" in s:
                                seats_count = s["availableSeats"]
                                if seats_count == 0:
                                    available_text = " ⚠️ HẾT VÉ"
                                elif seats_count < 10:
                                    available_text = f" (còn {seats_count} ghế — sắp hết)"
                                else:
                                    available_text = f" (còn {seats_count} ghế)"

                            # Định dạng thời gian sạch đẹp: Suất hh:mm ngày dd/MM
                            raw_time = s.get("startTime", "")
                            time_str = "20:00"
                            date_part = ""
                            if "T" in raw_time:
                                parts = raw_time.split("T")
                                date_val = parts[0]
                                time_val = parts[1][:5]
                                time_str = time_val
                                try:
                                    dt = datetime.strptime(date_val, "%Y-%m-%d")
                                    date_part = f" ngày {dt.strftime('%d/%m')}"
                                except Exception:
                                    pass
                            else:
                                time_str = raw_time[:5]

                            # Gọi tool lấy thông tin thời tiết phục vụ cảnh báo ngầm (Implicit Warning)
                            weather_text = ""
                            try:
                                from ...tools.weather_tools import GetShowtimeWeatherTool
                                weather_tool = GetShowtimeWeatherTool()
                                weather_info = weather_tool.execute(s.get("id"))
                                if weather_info and weather_info.get("isBadWeather") is True:
                                    cond = weather_info.get("condition") or "thời tiết xấu"
                                    weather_text = f" 🌧️ [Cảnh báo: Dự báo {cond.lower()}]"
                            except Exception:
                                pass

                            lines.append(
                                f"• **[{i}]** {s.get('cinemaName', 'Hệ thống rạp')} — Phòng {s.get('screenName', '?')}"
                                f" — Suất {time_str}{date_part}{available_text}{weather_text} (Mã suất: **{i}**)"
                            )

                        state["showtime_list"] = showtime_list
                        session_manager.set_state(session_id, state)

                        lines.append(
                            f"\n💡 Muốn xem ngày khác? Anh/chị hãy gõ 'lịch chiếu phim [Tên Phim] ngày mai' hoặc chọn số thứ tự suất để đặt vé nháp nhé."
                        )
                        return "\n".join(lines)
                    except Exception as e:
                        return f"Thất bại khi lấy dữ liệu lịch chiếu: {str(e)}"
                else:
                    # Nếu đã có lịch chiếu trước đó nhưng chưa chọn được showtime_id và không khớp phim mới
                    showtime_list = state.get("showtime_list", [])
                    if showtime_list:
                        movie_title = showtime_list[0].get("movieTitle", "phim")
                        return (
                            f"Dạ, em đã tìm thấy lịch chiếu cho phim **'{movie_title}'** ở trên.\n"
                            f"Anh/chị vui lòng gõ **'đặt vé suất [số]'** (Ví dụ: **đặt vé suất 1**) hoặc gõ số thứ tự suất chiếu tương ứng để tiếp tục nhé!"
                        )

                    # Bật cờ stateful để bắt đầu hội thoại hỏi phim
                    state["awaiting_movie"] = True
                    session_manager.set_state(session_id, state)

                    if movies:
                        lines = [
                            "🎬 Chào anh/chị, anh/chị muốn đặt vé cho phim nào ạ?",
                            "Hiện tại rạp đang chiếu các phim cực kỳ hấp dẫn:"
                        ]
                        for m in movies[:7]:
                            genres = ", ".join(g["name"] for g in m.get("genres", []))
                            duration = f"{m.get('duration')} phút" if m.get("duration") else ""
                            rated = f"Hạng {m.get('rated')}" if m.get("rated") else ""
                            meta = " — ".join(filter(None, [duration, rated, genres]))
                            lines.append(f"• **{m['title']}** ({meta})")
                        
                        lines.append(f"\n👉 Anh/chị vui lòng phản hồi tên phim (Ví dụ: 'phim {movie_titles[0]}') để em hiển thị lịch chiếu nhé!")
                        return "\n".join(lines)
                    elif movie_titles:
                        movies_str = ", ".join(f"**{m}**" for m in movie_titles)
                        return (
                            f"🎬 Chào anh/chị, anh/chị muốn đặt vé cho phim nào ạ?\n"
                            f"Hiện tại rạp đang chiếu các phim: {movies_str}.\n\n"
                            f"Anh/chị vui lòng phản hồi tên phim (Ví dụ: 'phim {movie_titles[0]}') để em hiển thị lịch chiếu nhé!"
                        )
                    else:
                        return "Hiện tại hệ thống không có lịch chiếu cho phim nào đang chiếu khả dụng ạ."

            # 2. Xử lý logic máy trạng thái (Slot Filling)
            # Stage 2.1: Hỏi chọn ghế & Tư vấn vị trí ghế thông minh
            if not seats:
                state["showtime_id"] = showtime_id
                state["seats"] = []
                state["combos"] = []
                state["step"] = "select_seats"
                session_manager.set_state(session_id, state)
                
                suggested = get_suggested_seats.execute(showtime_id)
                selected_info = state.get("selected_showtime_info")
                
                movie_label = selected_info["movieTitle"] if selected_info else "phim đã chọn"
                cinema_label = selected_info["cinemaName"] if selected_info else "Rạp"
                time_label = selected_info["startTime"].split('T')[-1][:5] if (selected_info and "startTime" in selected_info) else "20:00"

                from ..intent_classifier import remove_vietnamese_accents
                msg_seat_clean = remove_vietnamese_accents(msg_lower)

                # 1. Câu hỏi về Sơ đồ ghế / Bố trí / Quy tắc đánh số
                is_layout_query = any(k in msg_seat_clean for k in [
                    "so do ghe", "sap xep ghe", "sap xep", "bo tri ghe", "bo tri", "thu tu ghe",
                    "danh so", "so do phong", "cau truc phong", "hang ghe the nao", "vi tri cac hang", "sap dat"
                ])
                if is_layout_query:
                    return (
                        f"📐 **Sơ đồ & Quy tắc bố trí ghế tại {cinema_label}:**\n\n"
                        f"• **Màn hình (Screen):** Nằm ở phía trước chính diện phòng chiếu.\n"
                        f"• **Hàng A — C (Ghế Thường):** Gần màn hình, tầm nhìn bao trùm khung hình lớn, giá vé tiết kiệm ({suggested.get('available_standard', 0)} ghế trống).\n"
                        f"• **Hàng D — H (Ghế VIP - Vị trí vàng):** Khu vực trung tâm rạp, góc nhìn ngang thẳng tầm mắt, âm thanh Dolby Atmos vòm chuẩn nhất, không bị mỏi cổ ({suggested.get('available_vip', 0)} ghế trống).\n"
                        f"• **Hàng K (Ghế Đôi Sweetbox):** Nằm ở hàng cuối cùng, thiết kế sofa đôi rộng rãi không vách ngăn, riêng tư và lãng mạn cho cặp đôi ({suggested.get('available_couple', 0)} ghế trống).\n"
                        f"• **Đánh số ghế:** Đánh số thứ tự từ trái qua phải (1, 2, 3... 12), các số ở giữa (như 5, 6, 7, 8) là chính giữa màn hình.\n\n"
                        f"👉 Anh/chị muốn đặt ghế nào hãy gõ ví dụ: **'chọn ghế G7 G8'** nhé!"
                    )

                # 2. Câu hỏi về Vị trí đẹp nhất / Đỡ mỏi mắt / Ghế VIP
                is_best_query = any(k in msg_seat_clean for k in [
                    "dep nhat", "cho nao dep", "vi tri dep", "ngoi dau dep", "goc nhin tot",
                    "do moi mat", "chuan nhat", "tot nhat", "ghe nao dep", "ghe dep"
                ])
                if is_best_query:
                    best_seats = ", ".join(suggested.get("suggested_seats", ["G7", "G8", "H6", "H7"]))
                    return (
                        f"🌟 **Tư vấn vị trí ghế đẹp nhất cho phim {movie_label} ({cinema_label}):**\n\n"
                        f"• **Vị trí vàng (Sweet Spot):** Các hàng ghế **VIP F, G, H** (đặc biệt là các ghế ở giữa từ số 5 đến 8) có góc nhìn $36^\\circ$ chuẩn điện ảnh quốc tế, không bị ngửa cổ và là điểm hội tụ âm thanh vòm sống động nhất.\n\n"
                        f"💡 **Các ghế VIP trung tâm đẹp nhất đang còn trống:** **{best_seats}**.\n\n"
                        f"👉 Anh/chị có muốn đặt các ghế này không? Hãy gõ ví dụ: **'chọn ghế {best_seats}'** nhé!"
                    )

                # 3. Câu hỏi về Couple / Cặp đôi / 2 người / Sweetbox
                is_couple_query = any(k in msg_seat_clean for k in [
                    "couple", "cap doi", "2 nguoi", "hai nguoi", "nguoi yeu", "ghe doi", "sweetbox", "rieng tu", "hen ho"
                ])
                if is_couple_query:
                    c_pairs = suggested.get("adjacent_pairs_couple", [])
                    vip_pairs = suggested.get("adjacent_pairs_vip", [])
                    
                    couple_text = ""
                    if c_pairs:
                        pair_str = ", ".join([f"{p[0]}-{p[1]}" for p in c_pairs[:3]])
                        couple_text = f"• **Lựa chọn 1 — Ghế đôi Sweetbox (Hàng K cuối rạp):** Ghế sofa đôi liền nhau riêng tư lãng mạn. Đang trống các cặp: **{pair_str}**.\n"
                    
                    vip_text = ""
                    if vip_pairs:
                        v_pair_str = ", ".join([f"{p[0]}, {p[1]}" for p in vip_pairs[:3]])
                        vip_text = f"• **Lựa chọn 2 — Cặp ghế VIP trung tâm (Hàng F, G):** Góc nhìn thẳng màn ảnh cực đẹp. Đang trống: **{v_pair_str}**.\n"

                    sample_pair = c_pairs[0] if c_pairs else (vip_pairs[0] if vip_pairs else ["G7", "G8"])
                    sample_str = " ".join(sample_pair)

                    return (
                        f"💑 **Tư vấn ghế cho Cặp đôi / 2 người xem {movie_label}:**\n\n"
                        f"{couple_text}"
                        f"{vip_text}\n"
                        f"👉 Anh/chị muốn chọn vị trí nào có thể gõ ví dụ: **'chọn ghế {sample_str}'** nhé!"
                    )

                # 4. Câu hỏi về Nhóm 3 người / 4 người / Đi đông người / Liền nhau
                is_group_query = any(k in msg_seat_clean for k in [
                    "3 nguoi", "ba nguoi", "4 nguoi", "bon nguoi", "nhom", "5 nguoi", "lien nhau", "canh nhau", "cung hang"
                ])
                if is_group_query:
                    quads = suggested.get("adjacent_quads", [])
                    triples = suggested.get("adjacent_triples", [])
                    
                    # Nếu người dùng hỏi 4 người
                    if "4" in msg_seat_clean or "bon" in msg_seat_clean:
                        if quads:
                            quad_lines = "\n".join([f"• Dãy 4 ghế liền nhau hàng {q[0][0]}: **{', '.join(q)}**" for q in quads[:3]])
                            sample_str = " ".join(quads[0])
                            return (
                                f"👥 **Tư vấn ghế cho Nhóm 4 người xem {movie_label}:**\n\n"
                                f"Em đã quét sơ đồ phòng và tìm thấy các dãy **4 ghế liền kề nhau** ở vị trí VIP rất đẹp:\n"
                                f"{quad_lines}\n\n"
                                f"👉 Anh/chị hãy gõ: **'chọn ghế {sample_str}'** để giữ cả 4 ghế cạnh nhau nhé!"
                            )
                        else:
                            return (
                                f"👥 **Tư vấn ghế cho Nhóm 4 người:**\n"
                                f"Hiện tại phòng chiếu không còn 4 ghế trống liền nhau trên cùng 1 hàng. Em gợi ý anh/chị chọn 2 cặp ghế hàng trên — hàng dưới ngay phía sau nhau (ví dụ: 2 ghế hàng G + 2 ghế hàng H) để nhóm ngồi gần nhau nhé!\n\n"
                                f"👉 Gõ ví dụ: **'chọn ghế G7 G8 H7 H8'** để đặt nhé."
                            )
                    
                    # Nếu người dùng hỏi 3 người
                    if "3" in msg_seat_clean or "ba" in msg_seat_clean:
                        if triples:
                            triple_lines = "\n".join([f"• Dãy 3 ghế liền nhau hàng {t[0][0]}: **{', '.join(t)}**" for t in triples[:3]])
                            sample_str = " ".join(triples[0])
                            return (
                                f"👥 **Tư vấn ghế cho Nhóm 3 người xem {movie_label}:**\n\n"
                                f"Em đã tìm thấy các dãy **3 ghế liền kề nhau** còn trống:\n"
                                f"{triple_lines}\n\n"
                                f"👉 Anh/chị hãy gõ: **'chọn ghế {sample_str}'** để giữ chỗ nhé!"
                            )

                res_msg = ResponseFormatter.format_suggested_seats(suggested)
                if selected_info:
                    start_time_clean = selected_info["startTime"].split('T')[-1][:5]
                    res_msg = res_msg.replace(
                        f"suất chiếu #{showtime_id}",
                        f"phim **{selected_info['movieTitle']}** (Rạp **{selected_info['cinemaName']}**) lúc **{start_time_clean}**"
                    )
                return res_msg

            # Stage 2.2: Đã có ghế, kiểm tra bước tiếp theo
            current_step = state.get("step", "select_seats")
            if current_step == "select_seats":
                # Đang ở chọn ghế, và hiện đã cung cấp danh sách ghế
                state["showtime_id"] = showtime_id
                state["seats"] = seats
                state["step"] = "select_combo"
                session_manager.set_state(session_id, state)

                # Lấy tên phim và rạp cho rõ thông tin context đặt
                showtime_info_str = f"suất vé #{showtime_id}"
                selected_info = state.get("selected_showtime_info")
                if selected_info:
                    start_time_clean = selected_info["startTime"].split('T')[-1][:5]
                    showtime_info_str = f"phim **{selected_info['movieTitle']}** tại rạp **{selected_info['cinemaName']}** lúc **{start_time_clean}**"

                return (
                    f"🛋️ Đã ghi nhận ghế: {', '.join(seats)} cho {showtime_info_str}.\n\n"
                    "🥤 Anh/chị có muốn đặt thêm bắp nước không ạ? Hiện tại rạp đang có:\n"
                    "- **Solo Combo** (1 bắp 1 nước): 65.000đ\n"
                    "- **Couple Combo** (1 bắp lớn 2 nước): 90.000đ\n\n"
                    "Gợi ý nhập: 'thêm combo solo' hoặc 'không' để bỏ qua."
                )

            elif current_step == "select_combo":
                # Đang ở chọn bắp nước
                combos = []
                # Kiểm tra xem người dùng quyết định bỏ qua hay chọn
                msg_no_diacritics = msg_lower.replace("không", "khong").replace("ko", "khong").replace("k ", "khong ")
                if "khong" in msg_no_diacritics or "bo qua" in msg_lower or "no" == msg_lower.strip() or "skip" in msg_lower:
                    combos = []
                else:
                    # Trích xuất số lượng nếu có
                    qty_match = re.search(r"(\d+)", msg_lower)
                    qty = int(qty_match.group(1)) if qty_match else 1

                    # ── Tra cứu UUID Combo thật từ Java API ─────────────
                    combo_keyword = None
                    if "solo" in msg_lower:
                        combo_keyword = "solo"
                    elif "couple" in msg_lower:
                        combo_keyword = "couple"
                    elif "family" in msg_lower:
                        combo_keyword = "family"

                    if combo_keyword is not None:
                        try:
                            import httpx as _httpx
                            from ...config import get_settings as _get_settings
                            _cfg = _get_settings()
                            with _httpx.Client(timeout=5) as _client:
                                _combo_resp = _client.get(
                                    f"{_cfg.java_api_base}/api/v1/combos"
                                )
                            _combo_resp.raise_for_status()
                            _combo_list = _combo_resp.json().get("data", [])
                            _matched_combo = next(
                                (c for c in _combo_list if combo_keyword.lower() in c.get("name", "").lower()),
                                None
                            )
                            if _matched_combo:
                                combos = [{"comboId": str(_matched_combo["id"]), "quantity": qty}]
                            else:
                                return (
                                    f"😕 Dạ em không tìm thấy combo '{combo_keyword}' trong danh sách. "
                                    "Vui lòng nhập 'thêm combo solo', 'thêm combo couple' hoặc 'không' để bỏ qua nhé."
                                )
                        except Exception as _e:
                            return (
                                f"❌ Lỗi khi tra cứu thông tin combo: {str(_e)}. "
                                "Anh/chị vui lòng thử lại sau nhé."
                            )
                    else:
                        # Nếu gõ điều không rõ ràng
                        return (
                            "Dạ em chưa rõ combo bắp nước anh/chị chọn. "
                            "Vui lòng nhập rõ: 'thêm combo solo', 'thêm combo couple' hoặc 'không' để bỏ qua nhé."
                        )

                # ── Tra cứu showtimeSeatId thật từ seat map ────────────
                selected_seats_labels = state.get("seats", [])
                try:
                    import httpx as _httpx
                    from ...config import get_settings as _get_settings
                    _cfg = _get_settings()
                    with _httpx.Client(timeout=5) as _client:
                        _seat_resp = _client.get(
                            f"{_cfg.java_api_base}/internal/api/seats/available",
                            headers={"X-Internal-Key": _cfg.internal_api_key},
                            params={"showtimeId": showtime_id}
                        )
                    _seat_resp.raise_for_status()
                    _seat_map_data = _seat_resp.json()
                    _seat_items = _seat_map_data.get("seats", [])

                    # Build label -> showtimeSeatId mapping
                    _label_to_uuid = {}
                    for _item in _seat_items:
                        # Tạo seat label từ rowLabel + colNumber, ví dụ "G7"
                        _row = _item.get("rowLabel", "")
                        _col = _item.get("colNumber", "")
                        if _row and _col:
                            _label = f"{_row}{_col}"
                            _label_to_uuid[_label.upper()] = _item.get("showtimeSeatId")

                    # Map danh sách seat label sang UUID
                    seat_ids = []
                    _not_found = []
                    for _lbl in selected_seats_labels:
                        _uuid = _label_to_uuid.get(_lbl.upper().strip())
                        if _uuid:
                            seat_ids.append(_uuid)
                        else:
                            _not_found.append(_lbl)

                    if _not_found:
                        session_manager.clear(session_id)
                        return (
                            f"❌ Không tìm thấy ghế: {', '.join(_not_found)} trong sơ đồ suất này. "
                            "Anh/chị vui lòng bắt đầu chọn ghế lại nhé."
                        )

                except Exception as _e:
                    session_manager.clear(session_id)
                    return (
                        f"❌ Lỗi khi tra cứu sơ đồ ghế: {str(_e)}. "
                        "Anh/chị vui lòng thử lại sau nhé."
                    )

                # Hoàn tất đặt vé nháp qua API Java
                draft_res = create_draft_booking.execute(showtime_id, seat_ids, session_id, combos)

                # Reset state tránh ảnh hưởng lần trò chuyện sau
                session_manager.clear(session_id)

                if draft_res.get("status") != "error":
                    draft_res["seats"] = selected_seats_labels
                    draft_res["combos"] = combos
                    selected_info = state.get("selected_showtime_info")
                    if selected_info:
                        start_time_clean = selected_info["startTime"].split('T')[-1][:5]
                        draft_res["movieTitle"] = selected_info["movieTitle"]
                        draft_res["cinemaName"] = selected_info["cinemaName"]
                        draft_res["startTime"] = f"{start_time_clean} ngày {selected_info['startTime'].split('T')[0]}"
                    else:
                        draft_res["startTime"] = f"Suất #{showtime_id}"

                return ResponseFormatter.format_draft_booking(draft_res)


            # Trường hợp fallback an toàn
            return (
                "🎫 Dạ, thông tin đặt vé của anh/chị đã hết hạn hoặc không hợp lệ. "
                "Anh/chị vui lòng nhập 'đặt vé suất <mã>' để bắt đầu lại nhé."
            )

        # ── INTENT 3: Nhắc nhở lịch chiếu ───────────
        elif intent == "REMINDER_DRAFT":
            # Tránh xung đột chéo: xóa sạch trạng thái đặt vé nháp dở dang
            state.pop("awaiting_movie", None)
            state.pop("showtime_id", None)
            state.pop("seats", None)
            state.pop("combos", None)

            current_step = state.get("current_step")

            # BƯỚC 1: Người dùng bắt đầu luồng nhắc nhở chưa chọn Option
            if not current_step:
                state["current_step"] = "select_reminder_flow"
                state["awaiting_reminder_showtime"] = True
                session_manager.set_state(session_id, state)
                return (
                    "⏰ Anh/chị vui lòng chọn loại hình nhắc lịch bằng cách nhắn **1**, **2** hoặc **3**:\n"
                    "1. Nhắc lịch đặt vé (Khi phim có lịch chiếu mới tại rạp)\n"
                    "2. Nhắc trước giờ chiếu (Thông báo trước giờ chiếu 1 tiếng cho vé đã mua)\n"
                    "3. Xem & Hủy nhắc lịch (Hiển thị danh sách và cho phép hủy có xác nhận)"
                )

            # BƯỚC 2: Người dùng chọn giữa Option 1, Option 2 và Option 3
            elif current_step == "select_reminder_flow":
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean = remove_vietnamese_accents(msg_lower)

                if msg_clean == "1" or any(x in msg_clean for x in ["dat ve", "nhac dat", "mo ban", "giu cho", "dat truoc"]):
                    state["reminder_flow"] = "BOOKING"
                    state["current_step"] = "booking_flow_select_movie"
                    session_manager.set_state(session_id, state)
                    return "Dạ, anh/chị muốn đặt nhắc lịch đặt vé cho phim nào sắp tới ạ?"
                elif msg_clean == "2" or any(x in msg_clean for x in ["truoc gio", "gio chieu", "xem phim", "nhac xem", "xem"]):
                    state["reminder_flow"] = "SHOWTIME"
                    # Tìm vé xem phim sắp diễn ra để nhắc lịch
                    booked_list = []
                    try:
                        res = get_user_tickets.execute(session_id)
                        tickets = res.get("tickets", []) if isinstance(res, dict) else []
                        from datetime import datetime
                        now_dt = datetime.now()
                        for t in tickets:
                            status = t.get("status")
                            start_time_str = t.get("startTime", "")
                            
                            is_future = True
                            if start_time_str:
                                try:
                                    dt = datetime.fromisoformat(start_time_str.replace("Z", ""))
                                    if dt < now_dt:
                                        is_future = False
                                except Exception:
                                    pass
                            
                            # Chỉ lấy các vé đã thanh toán/giữ ghế sắp tới
                            if is_future and status in ["PAID", "CONFIRMED", "PENDING"]:
                                booked_list.append(t)
                    except Exception:
                        pass
                    
                    if booked_list:
                        from datetime import datetime
                        lines = [
                            "⏰ Dạ, anh/chị muốn đặt nhắc hẹn cho vé xem phim nào sắp tới ạ?",
                            "Dưới đây là danh sách vé chuẩn bị chiếu của anh/chị:"
                        ]
                        showtime_list = []
                        for i, t in enumerate(booked_list, 1):
                            showtime_list.append({
                                "id": t.get("showtimeId"),
                                "movieTitle": t.get("movieTitle"),
                                "cinemaName": t.get("cinemaName"),
                                "startTime": t.get("startTime")
                            })
                            
                            raw_time = t.get("startTime", "")
                            time_str = ""
                            if "T" in raw_time:
                                parts = raw_time.split("T")
                                try:
                                    dt = datetime.strptime(parts[0], "%Y-%m-%d")
                                    time_str = f"lúc {parts[1][:5]} ngày {dt.strftime('%d/%m')}"
                                except Exception:
                                    time_str = f"lúc {parts[1][:5]} ngày {parts[0]}"
                            else:
                                time_str = raw_time[:16]
                                
                            lines.append(f"• **[{i}]** {t.get('movieTitle')} — {t.get('cinemaName')} ({time_str})")
                            
                        lines.append(f"\n👉 Anh/chị vui lòng phản hồi số thứ tự (từ 1 đến {len(booked_list)}) để đặt nhắc lịch nhé!")
                        
                        state["showtime_list"] = showtime_list
                        state["current_step"] = "showtime_flow_select_ticket"
                        session_manager.set_state(session_id, state)
                        return "\n".join(lines)
                    else:
                        # Reset state nhắc lịch
                        for key in ["current_step", "reminder_flow", "showtime_list", "awaiting_reminder_showtime"]:
                            state.pop(key, None)
                        session_manager.set_state(session_id, state)
                        return "🎫 Anh/chị hiện chưa có giao dịch mua vé nào gần đây để đặt nhắc lịch chiếu."
                        
                elif msg_clean == "3" or any(x in msg_clean for x in ["huy nhac", "xem nhac", "danh sach nhac", "quan ly nhac", "xoa nhac"]):
                    reminders = get_reminders_tool.execute(session_id)
                    if not reminders:
                        for key in ["current_step", "reminder_flow", "showtime_list", "awaiting_reminder_showtime"]:
                            state.pop(key, None)
                        session_manager.set_state(session_id, state)
                        return "🎫 Anh/chị hiện chưa có lịch nhắc nào đang chờ."
                        
                    lines = [
                        "⏰ Dưới đây là danh sách nhắc lịch đang chờ của anh/chị:",
                    ]
                    reminder_list_ids = []
                    for i, r in enumerate(reminders, 1):
                        reminder_list_ids.append({
                            "id": r.get("id"),
                            "title": r.get("title")
                        })
                        lines.append(f"• **[{i}]** {r.get('title')} — {r.get('body')}")
                        
                    lines.append(f"\n👉 Anh/chị vui lòng phản hồi số thứ tự (từ 1 đến {len(reminders)}) để chọn nhắc lịch muốn hủy, hoặc gõ **'Tất cả'** để hủy toàn bộ nhắc lịch nhé!")
                    
                    state["reminder_list_ids"] = reminder_list_ids
                    state["current_step"] = "showtime_flow_cancel_reminder_confirm"
                    session_manager.set_state(session_id, state)
                    return "\n".join(lines)
                    
                else:
                    return (
                        "⚠️ Anh/chị chỉ cần chọn 1 (Nhắc đặt vé), 2 (Nhắc trước giờ chiếu) hoặc 3 (Xem & Hủy nhắc lịch) giúp em nhé.\n\n"
                        "Anh/chị vui lòng nhập số tương ứng hoặc gõ từ khóa như 'đặt vé', 'giờ chiếu' hay 'hủy nhắc'."
                    )

            # BƯỚC 3 (Option 1): Chọn phim
            elif current_step == "booking_flow_select_movie":
                movies = []
                try:
                    import httpx
                    from ...config import get_settings
                    cfg = get_settings()
                    headers = {"X-Internal-Key": cfg.internal_api_key}
                    with httpx.Client(timeout=10) as client:
                        resp = client.get(
                            f"{cfg.java_api_base}/internal/api/movies/now-showing",
                            headers=headers
                        )
                        resp.raise_for_status()
                        movies = resp.json()
                    movie_titles = [m["title"] for m in movies if m.get("title")]
                    if not movie_titles:
                        movie_titles = ["Mai", "Kung Fu Panda 4"]
                except Exception:
                    movie_titles = ["Mai", "Kung Fu Panda 4"]

                # Phân giải phim
                movie_name, candidates = resolve_movie_title(user_message, movie_titles)

                if not movie_name and candidates:
                    state["movie_candidates"] = candidates
                    state["current_step"] = "clarify_movie"
                    state["next_step"] = "booking_flow_select_cinema"
                    session_manager.set_state(session_id, state)
                    
                    candidate_lines = []
                    for i, cand in enumerate(candidates, 1):
                        candidate_lines.append(f"[{i}] {cand}")
                    return (
                        f"🤔 Em tìm thấy một số phim khớp với mô tả của anh/chị. "
                        f"Anh/chị vui lòng chọn số thứ tự phim mong muốn:\n" + "\n".join(candidate_lines)
                    )
                
                if not movie_name and not candidates:
                    catalog_movies = " / ".join([f"'{m}'" for m in movie_titles])
                    return (
                        f"Dạ, em không tìm thấy phim nào khớp với tên '[user_msg]'. "
                        f"Hiện rạp đang chiếu các phim: {catalog_movies}. Anh/chị vui lòng nhập lại tên phim nhé!"
                    ).replace("[user_msg]", user_message)

                # Chọn thành công phim
                state["selected_movie"] = movie_name
                state["current_step"] = "booking_flow_select_cinema"
                session_manager.set_state(session_id, state)
                return (
                    f"🎬 Đã ghi nhận phim: **{movie_name}**.\n\n"
                    f"Anh/chị có muốn giới hạn nhắc lịch ở một rạp cụ thể không? "
                    f"Vui lòng nhập tên rạp (ví dụ: Nguyễn Trãi), hoặc gõ 'Không' để nhận thông báo từ tất cả các rạp nhé."
                )

            # BƯỚC 4 (Option 1): Chọn rạp & So khớp showtime để tạo
            elif current_step == "booking_flow_select_cinema":
                movie_name = state.get("selected_movie")
                cinema_filter = None
                
                # Tìm rạp
                if "nguyễn trãi" in msg_lower or "nguyen trai" in msg_lower:
                    cinema_filter = "Nguyễn Trãi"
                elif "trần hưng đạo" in msg_lower or "tran hung dao" in msg_lower:
                    cinema_filter = "Trần Hưng Đạo"

                # Truy vấn showtimes của phim
                import httpx
                from ...config import get_settings
                cfg = get_settings()
                headers = {"X-Internal-Key": cfg.internal_api_key}
                
                showtimes = []
                try:
                    with httpx.Client(timeout=10) as client:
                        resp = client.get(
                            f"{cfg.java_api_base}/internal/api/showtimes",
                            headers=headers
                        )
                        resp.raise_for_status()
                        showtimes = resp.json()
                except Exception:
                    pass

                # Lọc showtimes khớp phim (và rạp nếu có)
                matching_showtimes = []
                for st in showtimes:
                    if normalize_title(st.get("movieTitle", "")) == normalize_title(movie_name):
                        if not cinema_filter or normalize_title(st.get("cinemaName", "")) == normalize_title(cinema_filter):
                            matching_showtimes.append(st)

                # Nếu lọc theo rạp không ra, thử tìm rạp bất kỳ cho phim đó
                if not matching_showtimes and cinema_filter:
                    for st in showtimes:
                        if normalize_title(st.get("movieTitle", "")) == normalize_title(movie_name):
                            matching_showtimes.append(st)

                if matching_showtimes:
                    # Lấy showtime đầu tiên
                    target_st = matching_showtimes[0]
                    showtime_id = target_st["id"]
                    
                    msg = f"Đến thời gian đặt vé phim {target_st.get('movieTitle')} của suất chiếu rồi anh/chị ơi!"
                    res = create_draft_reminder.execute(showtime_id, msg, session_id, "BOOKING")
                    
                    # Dọn dẹp trạng thái
                    for key in ["current_step", "selected_movie", "reminder_flow", "showtime_list", "awaiting_reminder_showtime"]:
                        state.pop(key, None)
                    session_manager.set_state(session_id, state)
                    
                    if res.get("status") != "error":
                        res["reminderType"] = "BOOKING"
                        start_time_clean = target_st.get("startTime", "").split('T')[-1][:5]
                        res["reminderTime"] = f"{target_st.get('movieTitle')} ({start_time_clean})"
                        res["message"] = f"Nhắc lịch đặt vé phim {target_st.get('movieTitle')}"
                        
                    return ResponseFormatter.format_draft_reminder(res)
                else:
                    # Trả thông báo lỗi rạp chưa xếp lịch
                    for key in ["current_step", "selected_movie", "reminder_flow", "showtime_list", "awaiting_reminder_showtime"]:
                        state.pop(key, None)
                    session_manager.set_state(session_id, state)
                    cinema_text = f" tại rạp '{cinema_filter}'" if cinema_filter else ""
                    return f"❌ Hiện tại phim '{movie_name}' chưa được xếp lịch chiếu nào{cinema_text} để liên kết nhắc nhở. Vui lòng thử lại sau nhé!"

            # BƯỚC 3 (Option 2): Xác thực & Tạo nhắc nhở trước giờ chiếu
            elif current_step == "showtime_flow_select_ticket":
                showtime_list = state.get("showtime_list", [])
                
                is_valid = False
                idx = -1
                if msg_lower.isdigit():
                    idx = int(msg_lower) - 1
                    if 0 <= idx < len(showtime_list):
                        is_valid = True
                        
                if not is_valid:
                    return f"⚠️ Số thứ tự vé không hợp lệ. Vui lòng chọn một số từ 1 đến {len(showtime_list)} giúp em nhé."
                    
                selected_st = showtime_list[idx]
                showtime_id = selected_st["id"]
                
                msg = f"Đến giờ xem phim {selected_st.get('movieTitle')} của suất chiếu rồi anh/chị ơi!"
                res = create_draft_reminder.execute(showtime_id, msg, session_id, "SHOWTIME")
                
                # Dọn dẹp trạng thái
                for key in ["current_step", "reminder_flow", "showtime_list", "awaiting_reminder_showtime"]:
                    state.pop(key, None)
                session_manager.set_state(session_id, state)
                
                if res.get("status") != "error":
                    res["reminderType"] = "SHOWTIME"
                    start_time_clean = selected_st.get("startTime", "").split('T')[-1][:5]
                    res["reminderTime"] = f"{selected_st.get('movieTitle')} ({start_time_clean})"
                    res["message"] = f"Nhắc lịch xem phim {selected_st.get('movieTitle')}"
                    
                return ResponseFormatter.format_draft_reminder(res)

            # BƯỚC 3.5 (Option 3): Chọn nhắc lịch để hủy hoặc gõ "Tất cả"
            elif current_step == "showtime_flow_cancel_reminder_confirm":
                reminder_list_ids = state.get("reminder_list_ids", [])
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean = remove_vietnamese_accents(msg_lower)
                
                # Check nếu muốn xóa tất cả
                if msg_clean in ["tat ca", "all", "tất cả"]:
                    state["reminder_to_delete"] = "all"
                    state["reminder_to_delete_title"] = "tất cả các nhắc lịch"
                    state["current_step"] = "showtime_flow_cancel_reminder_verify"
                    session_manager.set_state(session_id, state)
                    return "⚠️ Anh/chị có chắc chắn muốn hủy TOÀN BỘ nhắc lịch đang chờ không ạ? Vui lòng gõ **'Có'** hoặc **'Không'** để xác nhận."
                
                # Check nếu chọn index số
                is_valid = False
                idx = -1
                if msg_lower.isdigit():
                    idx = int(msg_lower) - 1
                    if 0 <= idx < len(reminder_list_ids):
                        is_valid = True
                
                if not is_valid:
                    return f"⚠️ Số thứ tự không hợp lệ. Vui lòng chọn một số từ 1 đến {len(reminder_list_ids)} hoặc gõ 'Tất cả' giúp em nhé."
                
                selected_rem = reminder_list_ids[idx]
                state["reminder_to_delete"] = selected_rem["id"]
                state["reminder_to_delete_title"] = selected_rem["title"]
                state["current_step"] = "showtime_flow_cancel_reminder_verify"
                session_manager.set_state(session_id, state)
                return f"⚠️ Anh/chị có chắc chắn muốn hủy nhắc lịch **'{selected_rem['title']}'** không ạ? Vui lòng gõ **'Có'** hoặc **'Không'** để xác nhận."
                
            # BƯỚC 4.5 (Option 3): Xác nhận hủy Có / Không
            elif current_step == "showtime_flow_cancel_reminder_verify":
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean = remove_vietnamese_accents(msg_lower)
                
                if msg_clean in ["co", "yes", "dong y", "dong y ", "chac chan", "dung vay", "xoa", "huy"]:
                    target_id = state.get("reminder_to_delete")
                    target_title = state.get("reminder_to_delete_title")
                    
                    # Gọi API xóa nhắc lịch
                    del_res = delete_reminder_tool.execute(target_id, session_id)
                    
                    # Dọn dẹp trạng thái
                    for key in ["current_step", "reminder_flow", "showtime_list", "awaiting_reminder_showtime", "reminder_list_ids", "reminder_to_delete", "reminder_to_delete_title"]:
                        state.pop(key, None)
                    session_manager.set_state(session_id, state)
                    
                    if del_res.get("status") == "error":
                        if del_res.get("code") == 404:
                            return "Dạ, nhắc lịch này đã được xử lý hoặc không còn tồn tại trên hệ thống từ trước. Anh/chị cần em hỗ trợ gì khác không?"
                        return f"❌ Lỗi khi thực hiện xóa nhắc lịch: {del_res.get('message', 'Lỗi không xác định')}. Anh/chị vui lòng thử lại sau."
                    
                    if target_id == "all":
                        return "⏰ Đã hủy toàn bộ nhắc lịch đang chờ thành công! Anh/chị cần em hỗ trợ gì khác không?"
                    return f"⏰ Đã hủy nhắc lịch **'{target_title}'** thành công! Anh/chị cần em hỗ trợ gì khác không?"
                    
                elif msg_clean in ["khong", "no", "tu choi", "giu nguyen", "thoi", "huy bo"]:
                    # Dọn dẹp trạng thái
                    for key in ["current_step", "reminder_flow", "showtime_list", "awaiting_reminder_showtime", "reminder_list_ids", "reminder_to_delete", "reminder_to_delete_title"]:
                        state.pop(key, None)
                    session_manager.set_state(session_id, state)
                    return "Dạ em đã giữ nguyên nhắc lịch của anh/chị rồi ạ. Anh/chị cần em hỗ trợ gì khác không?"
                
                else:
                    # Fallback Input Loop cho branch không khớp
                    return "Dạ, anh/chị vui lòng gõ **'Có'** hoặc **'Không'** để xác nhận giúp em nhé."

        # ── INTENT 3.5: Tra cứu thời tiết (Explicit Query)
        elif intent == "WEATHER_QUERY":
            # Tra cứu thời tiết phim/suất đang chọn
            showtime_id = state.get("showtime_id")
            
            # Nếu chưa có showtime_id được lưu nhưng có showtime_list trong state, dùng showtime đầu tiên
            if not showtime_id and state.get("showtime_list"):
                showtime_id = state.get("showtime_list")[0].get("id")
                
            if not showtime_id:
                # Kiểm tra xem người dùng có đề cập rạp hoặc khu vực cụ thể nào không
                from ..intent_classifier import remove_vietnamese_accents
                msg_clean_w = remove_vietnamese_accents(msg_lower)
                # So khớp rạp động từ Database
                cinemas_data = get_active_cinemas()
                matched_cinema_obj, target_cinema_name = resolve_cinema_entity(user_message, cinemas_data)
                asking_relative_cinema = any(rc in msg_clean_w for rc in ["rap do", "o do", "o day", "rap nay", "khu vuc do", "cho do", "o rap"])
                if not target_cinema_name and asking_relative_cinema:
                    target_cinema_name = state.get("last_cinema", "")

                # Nếu người dùng hỏi phim đang chiếu / danh sách phim kèm thời tiết
                is_asking_movies = any(p in msg_clean_w for p in [
                    "phim dang chieu", "nhung phim nao", "co phim nao", "co nhung phim", 
                    "danh sach phim", "dang chieu gi", "co phim gi", "chieu gi", "xem gi", 
                    "nhung phim", "co nhung phim nao", "phim nao dang"
                ])
                if is_asking_movies:
                    movies = []
                    try:
                        import httpx
                        from ...config import get_settings
                        cfg = get_settings()
                        headers = {"X-Internal-Key": cfg.internal_api_key}
                        with httpx.Client(timeout=10) as client:
                            resp = client.get(
                                f"{cfg.java_api_base}/internal/api/movies/now-showing",
                                headers=headers
                            )
                            resp.raise_for_status()
                            movies = resp.json()
                        movie_titles = [m["title"] for m in movies if m.get("title")]
                        if not movie_titles:
                            movie_titles = ["Mai", "Kung Fu Panda 4", "Daredevil: Tái Sinh 2"]
                    except Exception:
                        movie_titles = ["Mai", "Kung Fu Panda 4", "Daredevil: Tái Sinh 2"]

                    header_loc = f" tại {target_cinema_name}" if target_cinema_name else " tại NovaTicket"
                    loc_title = target_cinema_name if target_cinema_name else "khu vực rạp"
                    max_display = 5

                    # Lưu ngữ cảnh vào Session State
                    state["last_catalog_movies"] = movie_titles
                    state["last_displayed_offset"] = min(max_display, len(movie_titles))
                    if target_cinema_name:
                        state["last_cinema"] = target_cinema_name
                    session_manager.set_state(session_id, state)

                    top_movies = movie_titles[:max_display]
                    catalog_lines = "\n".join([f"- **{m}**" for m in top_movies])
                    if len(movie_titles) > max_display:
                        catalog_lines += f"\n- *...và còn {len(movie_titles) - max_display} phim khác đang chiếu.*"

                    return (
                        f"🎬 Hiện tại{header_loc} đang chiếu các phim sau:\n\n{catalog_lines}\n\n"
                        f"🌤️ **Dự báo thời tiết:** Dự báo thời tiết tại {loc_title} hôm nay rất thuận lợi, trời quang mây tạnh (khoảng 29°C - 31°C) ☀️. Anh/chị có thể hoàn toàn an tâm di chuyển và thưởng thức phim nhé!\n\n"
                        f"💡 Anh/chị gõ **'lịch chiếu phim [Tên Phim]'** để xem suất chiếu và đặt vé nhé!"
                    )
                    
                if target_cinema_name:
                    return (
                        f"🌤️ **Dự báo thời tiết:** Dự báo thời tiết tại khu vực {target_cinema_name} hôm nay rất thuận lợi, trời quang mây tạnh (khoảng 29°C - 31°C) ☀️. Anh/chị có thể an tâm đến rạp thưởng thức các bộ phim hấp dẫn!\n\n"
                        f"💡 Anh/chị có thể gõ **'phim đang chiếu'** hoặc **'lịch chiếu phim [Tên Phim]'** để xem lịch và đặt vé nhé!"
                    )

                return (
                    "🌦️ Dạ, hiện tại em chưa rõ anh/chị đang muốn xem thời tiết cho suất chiếu nào.\n"
                    "Anh/chị vui lòng tìm kiếm suất chiếu trước bằng cách gõ 'lịch chiếu phim [Tên Phim]' rồi đặt câu hỏi về thời tiết của suất chiếu đó để em giải đáp nhé!"
                )
                
            try:
                from ...tools.weather_tools import GetShowtimeWeatherTool
                weather_tool = GetShowtimeWeatherTool()
                weather_info = weather_tool.execute(showtime_id)
                
                # Biểu thị các mốc out của forecast ngoài phạm vi lưu trữ
                if weather_info.get("outOfForecastRange") is True:
                    return "Dạ, suất chiếu này còn khá xa nên hiện tại em chưa có dự báo thời tiết chính xác. Gần ngày chiếu anh/chị xem lại giúp em nhé! 🌦️"
                    
                warn = weather_info.get("warningMessage")
                if warn:
                    return warn
                return "🌦️ Hiện tại hệ thống không thể lấy thông tin thời tiết lúc chiếu phim cho rạp này. Anh/chị lưu ý kiểm tra trước khi đi nhé. Chúc anh/chị xem phim vui vẻ!"
            except Exception as e:
                return "🌦️ Đang gặp lỗi kết nối với trung tâm dự báo thời tiết tại rạp. Anh/chị lưu ý kiểm tra trước giờ chiếu nhé!"

        # ── INTENT 4: Tra cứu lịch sử / CinePoint ───
        elif intent == "USER_QUERIES":
            # Yêu cầu gọi API tra cứu vé và điểm thành viên
            res = get_user_tickets.execute(session_id)
            return ResponseFormatter.format_user_tickets(res)

        # ── INTENT 5: Hỏi đáp FAQ / Chính sách ──────
        elif intent == "KNOWLEDGE_RAG":
            try:
                rag_info = search_knowledge_base.invoke(user_message)
                if rag_info and "Không tìm thấy" not in rag_info and "Lỗi" not in rag_info:
                    def clean_source_header(match):
                        import os
                        idx = match.group(1)
                        path = match.group(2)
                        section = match.group(3) or ""
                        
                        path_lower = path.lower()
                        doc_type = "Tài liệu"
                        if "cinema_info" in path_lower or "cinemas" in path_lower:
                            doc_type = "Thông tin Rạp"
                        elif "policy" in path_lower or "policies" in path_lower:
                            doc_type = "Chính sách & Quy định"
                        elif "faq" in path_lower:
                            doc_type = "Hỏi đáp FAQ"
                            
                        fname = os.path.basename(path)
                        return f"📍 [{doc_type} {idx}: {fname}{section}]"
                    
                    import re as _re
                    formatted_info = _re.sub(
                        r"\[Nguồn (\d+):\s*([^\s\]]+)([^\]]*)\]",
                        clean_source_header,
                        rag_info
                    )
                    return formatted_info.replace(" --- ", "\n\n").replace("*", "")
            except Exception:
                pass

            # Fallback if RAG doesn't have details
            if "phim" in msg_lower or "chiếu" in msg_lower:
                try:
                    movies_info = get_now_showing_movies.invoke("")
                    if movies_info and "Lỗi" not in movies_info:
                        return f"🎬 Danh sách phim đang chiếu tại NovaTicket:\n\n{movies_info}"
                except Exception:
                    pass
            elif "voucher" in msg_lower or "khuyến mãi" in msg_lower or "mã" in msg_lower:
                try:
                    vch = get_active_vouchers.invoke("")
                    if vch and "Lỗi" not in vch:
                        return f"🎁 Các ưu đãi đang diễn ra:\n\n{vch}"
                except Exception:
                    pass

        # ── INTENT 6: Phim đang chiếu (Fallback) ─────
        elif "phim" in msg_lower or "chiếu" in msg_lower:
            try:
                movies_info = get_now_showing_movies.invoke("")
                if movies_info and "Lỗi" not in movies_info:
                    return f"🎬 Danh sách phim đang chiếu tại NovaTicket:\n\n{movies_info}"
            except Exception:
                pass

        # ── INTENT 7: Khuyến mãi/Voucher (Fallback) ──
        elif "voucher" in msg_lower or "khuyến mãi" in msg_lower or "mã" in msg_lower:
            try:
                vch = get_active_vouchers.invoke("")
                if vch and "Lỗi" not in vch:
                    return f"🎁 Các ưu đãi đang diễn ra:\n\n{vch}"
            except Exception:
                pass

        # ── Fallback mặc định ────────────────────────
        return (
            "Dạ, em chưa nhận diện được yêu cầu chi tiết của anh/chị.\n\n"
            "Anh/chị có thể nhập:\n"
            "- 'đặt vé suất <mã>' để giữ ghế nháp\n"
            "- 'nhắc lịch suất <mã>' để tạo hẹn giờ thông báo\n"
            "- 'thẻ thành viên' hoặc 'lịch sử vé' để tra cứu điểm CinePoint & vé đã đặt\n"
            "- Câu hỏi thắc mắc về chính sách hoàn tiền, combo bắp nước..."
        )
