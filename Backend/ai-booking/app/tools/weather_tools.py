import httpx
from .base_tool import BaseTool
from ..config import get_settings

cfg = get_settings()

class GetShowtimeWeatherTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="get_showtime_weather",
            description="Lấy dự báo thời tiết tại rạp chiếu phim trong khung giờ bắt đầu suất chiếu."
        )

    def execute(self, showtime_id: str) -> dict:
        fallback_data = {
            "condition": None,
            "temperature": None,
            "isBadWeather": False,
            "outOfForecastRange": False,
            "warningMessage": ""
        }
        if not showtime_id:
            return fallback_data

        try:
            url = f"{cfg.java_api_base}/internal/api/ai/weather/showtime/{showtime_id}"
            headers = {
                "X-Internal-Key": cfg.internal_api_key
            }
            with httpx.Client(timeout=10) as client:
                resp = client.get(url, headers=headers)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", fallback_data)
                return fallback_data
        except Exception:
            # Thu hồi lỗi êm dịu, không crash chatbot
            return fallback_data


class GetCinemaWeatherTool(BaseTool):
    def __init__(self):
        super().__init__(
            name="get_cinema_weather",
            description="Lấy dự báo thời tiết hiện tại/trong ngày tại một rạp chiếu phim theo cinema_id."
        )

    def execute(self, cinema_id: str) -> dict:
        fallback_data = {
            "condition": None,
            "temperature": None,
            "isBadWeather": False,
            "outOfForecastRange": False,
            "warningMessage": ""
        }
        if not cinema_id:
            return fallback_data

        try:
            url = f"{cfg.java_api_base}/internal/api/ai/weather/cinema/{cinema_id}"
            headers = {
                "X-Internal-Key": cfg.internal_api_key
            }
            with httpx.Client(timeout=10) as client:
                resp = client.get(url, headers=headers)
                resp.raise_for_status()
                res_body = resp.json()
                if res_body.get("success") is True or res_body.get("status") == "success":
                    return res_body.get("data", fallback_data)
                return fallback_data
        except Exception:
            return fallback_data

