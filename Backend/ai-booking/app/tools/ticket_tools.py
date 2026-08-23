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
            description="Tìm các vị trí ghế đẹp (ghế VIP, ghế trung tâm) còn trống của suất chiếu, đồng thời phân tích layout sơ đồ ghế."
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

            available_by_row = {}
            vip_seats_avail = []
            couple_seats_avail = []
            standard_seats_avail = []

            for seat in seats:
                if seat.get("status") == "AVAILABLE":
                    seat_type = seat.get("seatType")
                    row = str(seat.get("rowLabel", "")).upper()
                    col = seat.get("colNumber", 0)
                    label = seat.get("seatLabel") or f"{row}{col}"
                    
                    item = {
                        "row": row,
                        "col": col,
                        "label": label,
                        "type": seat_type,
                        "price": seat.get("price")
                    }

                    if row not in available_by_row:
                        available_by_row[row] = []
                    available_by_row[row].append(item)

                    if seat_type == "STANDARD":
                        available_standard += 1
                        standard_seats_avail.append(label)
                    elif seat_type == "VIP":
                        available_vip += 1
                        vip_seats_avail.append(label)
                    elif seat_type == "COUPLE":
                        available_couple += 1
                        couple_seats_avail.append(label)

            # Sắp xếp các ghế trong mỗi hàng theo số cột tăng dần
            for r in available_by_row:
                available_by_row[r].sort(key=lambda x: x["col"])

            # 1. Tìm ghế VIP trung tâm đẹp nhất (ưu tiên hàng G, F, H, E, D)
            best_center_vip = []
            for preferred_row in ["G", "F", "H", "E", "D"]:
                if preferred_row in available_by_row:
                    row_seats = available_by_row[preferred_row]
                    for s in row_seats:
                        if s["type"] == "VIP" and s["label"] not in best_center_vip:
                            best_center_vip.append(s["label"])

            # 2. Tìm các cặp 2 ghế liền kề (cho couple / 2 người)
            adjacent_pairs_vip = []
            adjacent_pairs_couple = []
            for r, r_seats in available_by_row.items():
                for i in range(len(r_seats) - 1):
                    if r_seats[i+1]["col"] == r_seats[i]["col"] + 1:
                        pair = [r_seats[i]["label"], r_seats[i+1]["label"]]
                        if r_seats[i]["type"] == "COUPLE":
                            adjacent_pairs_couple.append(pair)
                        elif r_seats[i]["type"] == "VIP":
                            adjacent_pairs_vip.append(pair)

            # 3. Tìm nhóm 3 ghế liền kề (cho nhóm 3 người)
            adjacent_triples = []
            for r, r_seats in available_by_row.items():
                for i in range(len(r_seats) - 2):
                    if (r_seats[i+1]["col"] == r_seats[i]["col"] + 1 and 
                        r_seats[i+2]["col"] == r_seats[i+1]["col"] + 1):
                        adjacent_triples.append([r_seats[i]["label"], r_seats[i+1]["label"], r_seats[i+2]["label"]])

            # 4. Tìm nhóm 4 ghế liền kề (cho nhóm 4 người)
            adjacent_quads = []
            for r, r_seats in available_by_row.items():
                for i in range(len(r_seats) - 3):
                    if (r_seats[i+1]["col"] == r_seats[i]["col"] + 1 and 
                        r_seats[i+2]["col"] == r_seats[i+1]["col"] + 1 and
                        r_seats[i+3]["col"] == r_seats[i+2]["col"] + 1):
                        adjacent_quads.append([
                            r_seats[i]["label"], r_seats[i+1]["label"], 
                            r_seats[i+2]["label"], r_seats[i+3]["label"]
                        ])

            suggested = best_center_vip[:4] if best_center_vip else (vip_seats_avail[:4] or standard_seats_avail[:4] or ["G7", "G8", "H6", "H7"])

            return {
                "status": "success",
                "showtime_id": showtime_id,
                "available_vip": available_vip,
                "available_standard": available_standard,
                "available_couple": available_couple,
                "suggested_seats": suggested,
                "best_center_vip": best_center_vip,
                "adjacent_pairs_vip": adjacent_pairs_vip,
                "adjacent_pairs_couple": adjacent_pairs_couple,
                "adjacent_triples": adjacent_triples,
                "adjacent_quads": adjacent_quads,
                "total_rows": res_body.get("totalRows", 10),
                "total_cols": res_body.get("totalCols", 12)
            }
        except Exception as e:
            return {"status": "error", "message": f"Lỗi lấy gợi ý ghế: {str(e)}"}
