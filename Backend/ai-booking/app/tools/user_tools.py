import httpx
from .base_tool import BaseTool
from ..config import get_settings

cfg = get_settings()

class GetUserTicketsTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="get_user_tickets",
            description="Tra cứu danh sách vé đã mua và tích điểm thành viên (CinePoint) của khách hàng."
        )

    def execute(self, session_id: str) -> dict:
        try:
            url = f"{cfg.java_api_base}/internal/api/ai/user/tickets"
            headers = {
                "X-Internal-Key": cfg.internal_api_key
            }
            if session_id:
                headers["X-Session-Id"] = session_id
                
            with httpx.Client(timeout=10) as client:
                resp = client.get(url, headers=headers)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", {})
                return {"status": "error", "message": res_body.get("message", "unknown error")}
        except Exception as e:
            return {"status": "error", "message": f"Lỗi tra cứu thông tin người dùng: {str(e)}"}
