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

    def execute(self, showtime_id: int, message: str, session_id: str, reminder_type: str = "SHOWTIME") -> dict:
        try:
            url = f"{cfg.java_api_base}/internal/api/ai/reminder/draft"
            payload = {
                "showtimeId": str(showtime_id),
                "reminderType": reminder_type
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


class GetRemindersTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="get_reminders",
            description="Lấy danh sách các nhắc lịch hiện có của user."
        )

    def execute(self, session_id: str) -> list:
        try:
            url = f"{cfg.java_api_base}/internal/api/ai/reminder/list"
            headers = {
                "X-Internal-Key": cfg.internal_api_key,
                "X-Session-Id": session_id
            }
            with httpx.Client(timeout=10) as client:
                resp = client.get(url, headers=headers)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", [])
                return []
        except Exception:
            return []


class DeleteReminderTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="delete_reminder",
            description="Xóa một nhắc lịch cụ thể bằng ID hoặc xóa tất cả bằng cách truyền 'all'."
        )

    def execute(self, reminder_id: str, session_id: str) -> dict:
        try:
            if reminder_id == "all":
                url = f"{cfg.java_api_base}/internal/api/ai/reminder/all"
            else:
                url = f"{cfg.java_api_base}/internal/api/ai/reminder/{reminder_id}"
                
            headers = {
                "X-Internal-Key": cfg.internal_api_key,
                "X-Session-Id": session_id
            }
            with httpx.Client(timeout=10) as client:
                resp = client.delete(url, headers=headers)
                if resp.status_code == 404:
                    return {"status": "error", "code": 404, "message": "Nhắc lịch này đã được xử lý hoặc không còn tồn tại"}
                if resp.status_code == 403:
                    return {"status": "error", "code": 403, "message": "Bạn không có quyền xóa nhắc lịch này"}
                resp.raise_for_status()
                return {"status": "success"}
        except Exception as e:
            return {"status": "error", "message": f"Lỗi xóa nhắc lịch: {str(e)}"}

