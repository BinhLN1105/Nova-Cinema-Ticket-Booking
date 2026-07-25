import httpx
from .base_tool import BaseTool
from ..config import get_settings

cfg = get_settings()

class CreateDraftReminderTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="create_draft_reminder",
            description="Thiết lập nhắc lịch chiếu phim nháp. Cần showtime_id và tin nhắn nhắc nhở."
        )

    def execute(self, showtime_id: int, message: str, session_id: str) -> dict:
        try:
            url = f"{cfg.java_api_base}/internal/api/ai/reminder/draft"
            payload = {
                "showtimeId": str(showtime_id)
            }
            headers = {
                "X-Internal-Key": cfg.internal_api_key
            }
            if session_id:
                headers["X-Session-Id"] = session_id
                
            with httpx.Client(timeout=10) as client:
                resp = client.post(url, headers=headers, json=payload)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", {})
                return {"status": "error", "message": res_body.get("message", "unknown error")}
        except Exception as e:
            return {"status": "error", "message": f"Lỗi tạo nhắc nhở nháp: {str(e)}"}
