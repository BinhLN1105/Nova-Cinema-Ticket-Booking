class ResponseFormatter:
    
    @staticmethod
    def format_draft_booking(data: dict) -> str:
        if data.get("status") == "error":
            return f"❌ Xin lỗi anh/chị, quá trình tạo đặt vé nháp gặp lỗi: {data.get('message')}"
        
        draft_id = data.get("bookingId") or data.get("draftId") or "N/A"
        movie = data.get("movieTitle") or "phim đã chọn"
        cinema = data.get("cinemaName") or "Hệ thống rạp"
        time = data.get("startTime") or "suất chiếu"
        seats = ", ".join(data.get("seats", [])) if data.get("seats") else "Ghế đã chọn"
        
        # Thêm hiển thị bắp nước combo nếu có
        combo_text = ""
        if data.get("combos"):
            parts = []
            for c in data.get("combos"):
                name = "Solo Combo" if "solo" in str(c.get("comboId")).lower() else "Couple Combo"
                parts.append(f"{name} (x{c['quantity']})")
            combo_text = f"\n- Bắp nước: {', '.join(parts)}"
            
        amount = f"{data.get('totalAmount'):,}đ" if data.get("totalAmount") else "N/A"
        
        return (
            f"🎫 Em đã tạo đặt vé nháp thành công cho anh/chị!\n\n"
            f"- Mã đặt vé tạm: #{draft_id}\n"
            f"- Phim: {movie}\n"
            f"- Rạp: {cinema}\n"
            f"- Suất chiếu: {time}\n"
            f"- Ghế đã chọn: {seats}{combo_text}\n"
            f"- Tổng tiền dự kiến: {amount}\n\n"
            f"👉 Anh/chị vui lòng kiểm tra màn hình và bấm nút 'Xác nhận thanh toán' để hoàn tất đặt vé nhé (ghế sẽ được giữ trong 10 phút)."
        )

    @staticmethod
    def format_suggested_seats(data: dict) -> str:
        if data.get("status") == "error":
            return f"❌ Lỗi khi tìm đề xuất ghế: {data.get('message')}"
            
        seats = ", ".join(data.get("suggested_seats", []))
        return (
            f"💺 Em đã kiểm tra sơ đồ ghế của suất chiếu #{data.get('showtime_id')}.\n"
            f"- Ghế thường trống: {data.get('available_standard', 0)} ghế\n"
            f"- Ghế VIP trống: {data.get('available_vip', 0)} ghế\n\n"
            f"💡 Đề xuất cho anh/chị các vị trí đẹp nhất (giữa rạp): **{seats}**.\n"
            f"Anh/chị có muốn tiến hành đặt các ghế đẹp này không?"
        )

    @staticmethod
    def format_draft_reminder(data: dict) -> str:
        if data.get("status") == "error":
            return f"❌ Lỗi khi đặt nhắc nhở: {data.get('message')}"
            
        reminder_id = data.get("reminderId") or "N/A"
        time = data.get("reminderTime") or "Suất chiếu"
        return (
            f"⏰ Đã tạo nhắc nhở lịch chiếu nháp thành công!\n\n"
            f"- Mã nhắc nhở: #{reminder_id}\n"
            f"- Nội dung: {data.get('message', 'Theo dõi lịch chiếu phim')}\n"
            f"- Thời gian: 15 phút trước suất chiếu ({time})\n\n"
            f"👉 Em sẽ gửi thông báo đẩy (push notification) tới ứng dụng của anh/chị khi suất chiếu chuẩn bị bắt đầu."
        )

    @staticmethod
    def format_user_tickets(data: dict) -> str:
        if data.get("status") == "error":
            return f"❌ Không thể tra cứu thông tin vé: {data.get('message')}"
            
        tickets = data.get("tickets", [])
        points = data.get("cinePoints", 0)
        rank = data.get("rank", "Thành viên")
        
        header = f"👤 Thông tin thẻ CinePoint của anh/chị:\n- Xếp hạng: {rank}\n- Tích lũy: {points} CinePoints\n\n"
        
        if not tickets:
            return header + "🎫 Anh/chị hiện chưa có giao dịch mua vé nào gần đây."
            
        lines = [header + "🎫 Lịch sử đặt vé gần nhất của anh/chị:"]
        for t in tickets[:3]:
            seats = ", ".join(t.get("seats", []))
            lines.append(
                f"• [{t.get('bookingId')}] {t.get('movieTitle')} — {t.get('cinemaName')} — Suất {t.get('startTime')} — Ghế {seats} ({t.get('status')})"
            )
        return "\n".join(lines)
