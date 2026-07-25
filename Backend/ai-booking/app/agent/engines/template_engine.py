import re
from .base_engine import BaseChatEngine
from .response_formatter import ResponseFormatter
from ...tools import create_draft_booking, get_suggested_seats, create_draft_reminder, get_user_tickets
from ...agent.tools import search_knowledge_base, get_now_showing_movies, get_active_vouchers
from ..intent_classifier import IntentClassifier
from ..state import session_manager

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

class TemplateEngine(BaseChatEngine):
    def __init__(self):
        self.classifier = IntentClassifier()

    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        intent = self.classifier.classify(user_message)
        msg_lower = user_message.lower().strip()
        state = session_manager.get_state(session_id)

        # Nếu người dùng muốn tra cứu phim mới hoặc lịch chiếu phim mới, hãy reset trạng thái đặt vé hiện tại
        if "phim" in msg_lower or "lịch chiếu" in msg_lower or "lich chieu" in msg_lower:
            if not any(x in msg_lower for x in ["ghế", "ghe", "bắp", "bap", "nước", "nuoc"]):
                state.pop("showtime_id", None)
                state.pop("seats", None)
                state.pop("combos", None)
                state.pop("step", None)
                state.pop("selected_showtime_info", None)
                session_manager.set_state(session_id, state)

        # Override intent statefully
        has_active_showtime = state.get("showtime_id") is not None
        is_awaiting_movie = state.get("awaiting_movie") is True
        has_showtime_list = bool(state.get("showtime_list"))
        
        is_selecting_showtime = False
        if has_showtime_list:
            if msg_lower.isdigit():
                is_selecting_showtime = True
            elif re.search(r"\b(?:suất chiếu|suat chieu|suất|suat|mã suất|ma suat|mã|ma|chọn|chon|số|so)\b\s*\d+", msg_lower):
                is_selecting_showtime = True
            elif len(re.findall(r"\b\d+\b", msg_lower)) == 1:
                is_selecting_showtime = True
            elif any(x in msg_lower for x in ["đặt vé", "dat ve", "đặt", "dat", "suất", "suat", "chọn", "chon"]):
                if not any(x in msg_lower for x in ["hoàn vé", "hoan ve", "hủy vé", "huy ve", "chính sách", "chinh sach", "bắp nước", "bap nuoc", "combo"]):
                    is_selecting_showtime = True

        if intent not in ["BOOKING_DRAFT", "REMINDER_DRAFT"]:
            if has_active_showtime or is_awaiting_movie or is_selecting_showtime:
                intent = "BOOKING_DRAFT"

        # ── INTENT 1: Chào hỏi ──────────────────────
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
        elif intent == "BOOKING_DRAFT" or (session_manager.get_state(session_id).get("showtime_id") is not None):
            state = session_manager.get_state(session_id)

            # 1. Trích xuất thông tin đầu vào
            showtime_match = re.search(r"\b(?:suất chiếu|suat chieu|suất|suat|mã suất|ma suat|mã|ma|chọn|chon|số|so)\b\s*([a-zA-Z0-9\-]+)", msg_lower)
            showtime_input = None
            if showtime_match:
                candidate = showtime_match.group(1)
                if candidate.isdigit() or len(candidate) > 5:
                    showtime_input = candidate
            
            showtime_list = state.get("showtime_list", [])
            has_list = bool(showtime_list)

            # Fallback 1: Nếu không có từ khóa nhưng msg chứa duy nhất 1 số nguyên dương (chỉ áp dụng nếu sẵn list trong state)
            if not showtime_input and has_list:
                numbers = re.findall(r"\b\d+\b", msg_lower)
                if len(numbers) == 1:
                    showtime_input = numbers[0]
            
            # Fallback 2: Nếu msg chỉ gồm mỗi chữ số (chỉ áp dụng nếu sẵn list trong state)
            if not showtime_input and msg_lower.isdigit() and has_list:
                showtime_input = msg_lower

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

                # 2. Trích xuất phim dựa trên từ khóa trong tin nhắn
                movie_name = None
                movie_match = re.search(r"phim\s+([a-zA-Z0-9\s_:\-]+)", msg_lower)
                if movie_match:
                    candidate = movie_match.group(1).strip().lower()
                    for m in movie_titles:
                        if candidate in m.lower() or m.lower() in candidate:
                            movie_name = m
                            break

                if not movie_name:
                    from ..intent_classifier import remove_vietnamese_accents
                    msg_clean = remove_vietnamese_accents(msg_lower)
                    for m in movie_titles:
                        m_clean = remove_vietnamese_accents(m.lower())
                        if m_clean in msg_clean:
                            movie_name = m
                            break

                cinema_name = ""
                if "nguyễn trãi" in msg_lower or "nguyen trai" in msg_lower:
                    cinema_name = "Nguyễn Trãi"
                elif "trần hưng đạo" in msg_lower or "tran hung dao" in msg_lower:
                    cinema_name = "Trần Hưng Đạo"

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

                            lines.append(
                                f"• **[{i}]** {s.get('cinemaName', 'Hệ thống rạp')} — Phòng {s.get('screenName', '?')}"
                                f" — Suất {time_str}{date_part}{available_text} (Mã suất: **{i}**)"
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

                    if movie_titles:
                        movies_str = ", ".join(f"**{m}**" for m in movie_titles)
                        return (
                            f"🎬 Chào anh/chị, anh/chị muốn đặt vé cho phim nào ạ?\n"
                            f"Hiện tại rạp đang chiếu các phim: {movies_str}.\n\n"
                            f"Anh/chị vui lòng phản hồi tên phim (Ví dụ: 'phim {movie_titles[0]}') để em hiển thị lịch chiếu nhé!"
                        )
                    else:
                        return "Hiện tại hệ thống không có lịch chiếu cho phim nào đang chiếu khả dụng ạ."

            # 2. Xử lý logic máy trạng thái (Slot Filling)
            # Stage 2.1: Hỏi chọn ghế nếu chưa chọn ghế
            if not seats:
                state["showtime_id"] = showtime_id
                state["seats"] = []
                state["combos"] = []
                state["step"] = "select_seats"
                session_manager.set_state(session_id, state)
                
                suggested = get_suggested_seats.execute(showtime_id)
                res_msg = ResponseFormatter.format_suggested_seats(suggested)
                selected_info = state.get("selected_showtime_info")
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
                            _combo_resp = _httpx.get(
                                f"{_cfg.java_api_base}/api/v1/combos",
                                timeout=5
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
                    _seat_resp = _httpx.get(
                        f"{_cfg.java_api_base}/internal/api/seats/available",
                        headers={"X-Internal-Key": _cfg.internal_api_key},
                        params={"showtimeId": showtime_id},
                        timeout=5
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
            showtime_match = re.search(r"(?:suất|suat|mã|ma)\s*([a-zA-Z0-9\-]+)", msg_lower)
            showtime_input = showtime_match.group(1) if showtime_match else None
            
            showtime_id = None
            if showtime_input:
                showtime_list = state.get("showtime_list", [])
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
                            "⏰ Hiện tại hệ thống chưa lưu bộ nhớ lịch chiếu phim gần nhất.\n"
                            "Anh/chị vui lòng nhập 'lịch chiếu phim [Tên Phim]' trước để có danh sách suất chiếu nhé!"
                        )
                else:
                    showtime_id = showtime_input
            
            if not showtime_id:
                return (
                    "⏰ Dạ, anh/chị muốn đặt nhắc hẹn cho suất chiếu nào ạ?\n"
                    "Anh/chị vui lòng nhập dạng: 'nhắc lịch suất 1' (1 là số thứ tự suất chiếu từ danh sách)."
                )

            selected_info = state.get("selected_showtime_info")
            movie_str = ""
            if selected_info:
                movie_str = f" phim {selected_info['movieTitle']}"
            msg = f"Đến giờ xem{movie_str} của suất chiếu rồi anh/chị ơi!"
            res = create_draft_reminder.execute(showtime_id, msg, session_id)
            if res.get("status") != "error":
                if selected_info:
                    start_time_clean = selected_info["startTime"].split('T')[-1][:5]
                    res["reminderTime"] = f"{selected_info['movieTitle']} ({start_time_clean})"
                else:
                    res["reminderTime"] = f"Suất #{showtime_id}"
            return ResponseFormatter.format_draft_reminder(res)

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
                    return rag_info.replace("[Nguồn ", "📍 [Thông tin Rạp ").replace(" --- ", "\n\n").replace("*", "")
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
