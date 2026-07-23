from .base_engine import BaseChatEngine
from .response_formatter import ResponseFormatter
from ...tools import create_draft_booking, get_suggested_seats, create_draft_reminder, get_user_tickets
from ...agent.tools import search_knowledge_base, get_now_showing_movies, get_active_vouchers
from ..intent_classifier import IntentClassifier
from ..state import session_manager

class TemplateEngine(BaseChatEngine):
    def __init__(self):
        self.classifier = IntentClassifier()

    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        intent = self.classifier.classify(user_message)
        msg_lower = user_message.lower().strip()

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
            import re
            state = session_manager.get_state(session_id)

            # 1. Trích xuất thông tin đầu vào
            showtime_match = re.search(r"(?:suất|suat|mã|ma)\s*(\d+)", msg_lower)
            seats_match = re.findall(r"[a-jA-J]\d+", msg_lower)
            
            showtime_id = int(showtime_match.group(1)) if showtime_match else state.get("showtime_id")
            seats = [s.upper() for s in seats_match] if seats_match else state.get("seats", [])
            
            # Nếu chưa có suất chiếu và cũng không tra cứu được showtime_id trong state
            if not showtime_id:
                # Kiểm tra lọc suất chiếu theo tên rạp, phim
                movie_name = None
                movie_match = re.search(r"phim\s+([a-zA-Z0-9\s_]+)", msg_lower)
                if movie_match:
                    movie_name = movie_match.group(1).strip()
                else:
                    if "mai" in msg_lower:
                        movie_name = "Mai"
                    elif "panda" in msg_lower or "kung fu" in msg_lower:
                        movie_name = "Kung Fu Panda 4"

                cinema_name = ""
                if "nguyễn trãi" in msg_lower or "nguyen trai" in msg_lower:
                    cinema_name = "Nguyễn Trãi"
                elif "trần hưng đạo" in msg_lower or "tran hung dao" in msg_lower:
                    cinema_name = "Trần Hưng Đạo"

                date = ""
                date_match = re.search(r"(\d{2}/\d{2}/\d{4})", msg_lower)
                if date_match:
                    date = date_match.group(1)

                if movie_name or cinema_name or "lịch chiếu" in msg_lower or "lich chieu" in msg_lower:
                    from ...agent.tools import get_showtimes
                    title_to_query = movie_name if movie_name else "Mai"
                    showtimes_info = get_showtimes.func(movie_title=title_to_query, cinema_name=cinema_name, date=date)
                    return showtimes_info

                return (
                    "🎫 Dạ, để hỗ trợ đặt đặt vé nháp nhanh, anh/chị vui lòng cung cấp **Mã Suất Chiếu**.\n"
                    "Ví dụ gõ: 'đặt vé suất 125' hoặc 'đặt vé suất 125 ghế G7 G8'."
                )

            # 2. Xử lý logic máy trạng thái (Slot Filling)
            # Stage 2.1: Hỏi chọn ghế nếu chưa chọn ghế
            if not seats:
                state["showtime_id"] = showtime_id
                state["seats"] = []
                state["combos"] = []
                state["step"] = "select_seats"
                session_manager.set_state(session_id, state)
                
                suggested = get_suggested_seats.execute(showtime_id)
                return ResponseFormatter.format_suggested_seats(suggested)

            # Stage 2.2: Đã có ghế, kiểm tra bước tiếp theo
            current_step = state.get("step", "select_seats")
            if current_step == "select_seats":
                # Đang ở chọn ghế, và hiện đã cung cấp danh sách ghế
                state["showtime_id"] = showtime_id
                state["seats"] = seats
                state["step"] = "select_combo"
                session_manager.set_state(session_id, state)

                return (
                    f"🛋️ Đã ghi nhận ghế: {', '.join(seats)} cho suất vé #{showtime_id}.\n\n"
                    "🥤 Anh/chị có muốn đặt thêm bắp nước không ạ? Hiện tại rạp đang có:\n"
                    "- **Solo Combo** (1 bắp 1 nước): 65.000đ\n"
                    "- **Couple Combo** (1 bắp lớn 2 nước): 90.000đ\n\n"
                    "Gợi ý nhập: 'thêm combo solo' hoặc 'không' để bỏ qua."
                )

            elif current_step == "select_combo":
                # Đang ở chọn bắp nước
                combos = []
                # Kiểm tra xem người dùng quyết định bỏ qua hay chọn
                if "khong" in msg_lower or "bo qua" in msg_lower or "no" in msg_lower or "skip" in msg_lower:
                    combos = []
                else:
                    # Trích xuất số lượng nếu có
                    qty_match = re.search(r"(\d+)", msg_lower)
                    qty = int(qty_match.group(1)) if qty_match else 1
                    
                    if "solo" in msg_lower:
                        # Mượn một id giả lập làm mã combo
                        combos = [{"comboId": "combo_solo_id", "quantity": qty}]
                    elif "couple" in msg_lower:
                        combos = [{"comboId": "combo_couple_id", "quantity": qty}]
                    else:
                        # Nếu gõ điều không rõ ràng
                        return (
                            "Dạ em chưa rõ combo bắp nước anh/chị chọn. "
                            "Vui lòng nhập rõ: 'thêm combo solo' hoặc 'không' để bỏ qua nhé."
                        )

                # Hoàn tất đặt vé nháp qua API Java
                seat_ids = [ord(s[0].upper()) * 100 + int(s[1:]) for s in state["seats"]]
                draft_res = create_draft_booking.execute(showtime_id, seat_ids, session_id, combos)

                # Reset state tránh ảnh hưởng lần trò chuyện sau
                session_manager.clear(session_id)

                if draft_res.get("status") != "error":
                    draft_res["seats"] = seats
                    draft_res["combos"] = combos
                    draft_res["startTime"] = f"Suất #{showtime_id}"

                return ResponseFormatter.format_draft_booking(draft_res)

            # Trường hợp fallback an toàn
            return (
                "🎫 Dạ, thông tin đặt vé của anh/chị đã hết hạn hoặc không hợp lệ. "
                "Anh/chị vui lòng nhập 'đặt vé suất <mã>' để bắt đầu lại nhé."
            )

        # ── INTENT 3: Nhắc nhở lịch chiếu ───────────
        elif intent == "REMINDER_DRAFT":
            import re
            showtime_match = re.search(r"(?:suất|suat|mã|ma)\s*(\d+)", msg_lower)
            showtime_id = int(showtime_match.group(1)) if showtime_match else None
            
            if not showtime_id:
                return (
                    "⏰ Dạ, anh/chị muốn đặt nhắc hẹn cho suất chiếu nào ạ?\n"
                    "Anh/chị vui lòng nhập dạng: 'nhắc lịch suất 125' (125 là mã suất chiếu)."
                )
                
            msg = f"Đến giờ xem phim của suất chiếu #{showtime_id} rồi anh/chị ơi!"
            res = create_draft_reminder.execute(showtime_id, msg, session_id)
            if res.get("status") != "error":
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
