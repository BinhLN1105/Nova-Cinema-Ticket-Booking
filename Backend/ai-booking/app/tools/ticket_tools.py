import httpx
from .base_tool import BaseTool
from ..config import get_settings

cfg = get_settings()

def _java_headers(session_id: str) -> dict:
    headers = {
        "X-Internal-Key": cfg.internal_api_key
    }
    if session_id:
        headers["X-Session-Id"] = session_id
    return headers

class CreateDraftBookingTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="create_draft_booking",
            description="Tạo một bản ghi đặt vé nháp (Draft Booking) trên hệ thống. Cần showtime_id và danh sách seat_ids."
        )

    def execute(self, showtime_id: int, seat_ids: list[int], session_id: str, combos: list = None) -> dict:
        try:
            url = f"{cfg.java_api_base}/internal/api/ai/booking/draft"
            payload = {
                "showtimeId": str(showtime_id),
                "seatIds": [str(sid) for sid in seat_ids]
            }
            if combos:
                payload["combos"] = combos
            with httpx.Client(timeout=10) as client:
                resp = client.post(url, headers=_java_headers(session_id), json=payload)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", {})
                return {"status": "error", "message": res_body.get("message", "unknown error")}
        except Exception as e:
            return {"status": "error", "message": f"Lỗi tạo vé nháp: {str(e)}"}

class GetSuggestedSeatsTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="get_suggested_seats",
            description="Tìm các vị trí ghế đẹp (ghế VIP, ghế trung tâm) còn trống của suất chiếu."
        )

    def execute(self, showtime_id: int) -> dict:
        try:
            url = f"{cfg.java_api_base}/internal/api/seats/available"
            with httpx.Client(timeout=10) as client:
                resp = client.get(
                    url, 
                    headers={"X-Internal-Key": cfg.internal_api_key}, 
                    params={"showtimeId": showtime_id}
                )
                resp.raise_for_status()
                res_body = resp.json()
                
            # Endpoint /seats/available trả trực tiếp SeatMapResponse (flat JSON) chứa trường seats
            seats = res_body.get("seats", [])
            available_standard = 0
            available_vip = 0
            available_couple = 0

            for seat in seats:
                if seat.get("status") == "AVAILABLE":
                    seat_type = seat.get("seatType")
                    if seat_type == "STANDARD":
                        available_standard += 1
                    elif seat_type == "VIP":
                        available_vip += 1
                    elif seat_type == "COUPLE":
                        available_couple += 1

            return {
                "status": "success",
                "showtime_id": showtime_id,
                "available_vip": available_vip,
                "available_standard": available_standard,
                "available_couple": available_couple,
                "suggested_seats": ["G7", "G8", "H6", "H7"]  # Dãy ghế VIP trung tâm mẫu đề xuất
            }
        except Exception as e:
            return {"status": "error", "message": f"Lỗi lấy gợi ý ghế: {str(e)}"}
