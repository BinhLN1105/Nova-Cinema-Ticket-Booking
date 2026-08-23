"""
app/agent/engines/llm_engine.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
OpenRouter LLM Engine cho NovaTicket Enterprise Chatbot.
- Chuẩn OpenAI-compatible endpoint qua OpenRouter API.
- Nhiệt độ cấu hình 0.0 (Strict Zero-Hallucination).
- Tích hợp Grounding RAG Context & Multi-turn Session Memory.
- Tự động ném lỗi để tầng Chatbot kích hoạt Fallback khi bị Rate Limit (429) hoặc lỗi mạng.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

import datetime
import logging
import httpx
from .base_engine import BaseChatEngine
from ...config import get_settings
from ...agent.tools import search_knowledge_base, get_now_showing_movies

logger = logging.getLogger(__name__)
cfg = get_settings()

SYSTEM_PROMPT = """Bạn là Nova — trợ lý AI thông minh của NovaTicket, nền tảng đặt vé xem phim hàng đầu Việt Nam.

## Nhiệm vụ của bạn:
Hỗ trợ khách hàng tra cứu thông tin phim, lịch chiếu, giá vé, chính sách ưu đãi một cách nhanh chóng, chính xác và thân thiện.

## Nguyên tắc cốt lõi (Chuẩn Doanh Nghiệp):
1. TRUNG THỰC TUYỆT ĐỐI: Chỉ trả lời dựa trên dữ liệu ngữ cảnh được cung cấp bên dưới. TUYỆT ĐỐI KHÔNG tự bịa đặt suất chiếu, giá vé, mã giảm giá hay thông tin không có thật.
2. NẾU KHÔNG CÓ THÔNG TIN: Hãy trả lời lịch sự rằng hiện chưa có thông tin đó và hướng dẫn khách hàng cách kiểm tra (ví dụ: gõ 'lịch chiếu phim [Tên Phim]' hoặc liên hệ hotline).
3. ĐỊNH DẠNG RÕ RÀNG: Dùng dấu gạch đầu dòng (-) hoặc số thứ tự (1, 2, 3) để liệt kê thông tin.
4. GIỌNG ĐIỆU: Thân thiện, chu đáo, xưng "em" và gọi khách là "anh/chị".
"""

class LlmEngine(BaseChatEngine):
    def __init__(self):
        # Lưu trữ lịch sử hội thoại trượt theo từng session: list of {"role": "user"|"assistant", "content": str}
        self._session_histories: dict[str, list[dict]] = {}

    def _get_history(self, session_id: str) -> list[dict]:
        if session_id not in self._session_histories:
            self._session_histories[session_id] = []
        return self._session_histories[session_id]

    def _append_history(self, session_id: str, role: str, content: str):
        history = self._get_history(session_id)
        history.append({"role": role, "content": content})
        # Giữ tối đa 6 lượt hội thoại gần nhất (12 tin nhắn)
        if len(history) > 12:
            self._session_histories[session_id] = history[-12:]

    def _call_openrouter(self, messages: list[dict]) -> str:
        """Gửi request tới OpenRouter API với cơ chế tự động thử lần lượt các candidate models khi gặp Rate Limit hoặc lỗi"""
        api_key = cfg.openrouter_api_key or cfg.openai_api_key
        if not api_key:
            raise ValueError("Chưa cấu hình OPENROUTER_API_KEY")

        base_url = cfg.openrouter_base_url.rstrip("/")
        url = f"{base_url}/chat/completions"
        temperature = float(cfg.llm_temperature)

        headers = {
            "Authorization": f"Bearer {api_key}",
            "HTTP-Referer": "https://novaticket.vn",
            "X-Title": "NovaTicket Enterprise Assistant",
            "Content-Type": "application/json"
        }

        # 1. Tập hợp danh sách các model cần thử nghiệm theo thứ tự ưu tiên
        candidate_models = []
        if cfg.llm_model:
            candidate_models.append(cfg.llm_model.strip())

        fallback_models_str = getattr(cfg, "llm_fallback_models", "")
        if fallback_models_str:
            for m in fallback_models_str.split(","):
                m_clean = m.strip()
                if m_clean and m_clean not in candidate_models:
                    candidate_models.append(m_clean)

        if not candidate_models:
            candidate_models = ["meta-llama/llama-3.3-70b-instruct:free"]

        last_error = None
        timeout = float(getattr(cfg, "llm_timeout", 8.0))
        max_tokens = int(getattr(cfg, "llm_max_tokens", 1024))

        with httpx.Client(timeout=timeout) as client:
            for idx, model in enumerate(candidate_models):
                try:
                    logger.info(f"[OpenRouter LLM] [{idx + 1}/{len(candidate_models)}] Calling model '{model}' with temp={temperature}, timeout={timeout}s")
                    payload = {
                        "model": model,
                        "messages": messages,
                        "temperature": temperature,
                        "max_tokens": max_tokens
                    }
                    resp = client.post(url, headers=headers, json=payload)

                    if resp.status_code == 200:
                        data = resp.json()
                        choices = data.get("choices", [])
                        if choices:
                            reply = choices[0].get("message", {}).get("content", "").strip()
                            if reply:
                                logger.info(f"[OpenRouter LLM] Succeeded with model '{model}'")
                                return reply

                    err_msg = f"HTTP {resp.status_code}: {resp.text}"
                    logger.warning(f"[OpenRouter LLM] Model '{model}' failed ({err_msg}). Trying next candidate...")
                    last_error = RuntimeError(f"Model '{model}' failed ({err_msg})")
                except Exception as e:
                    logger.warning(f"[OpenRouter LLM] Model '{model}' exception: {e}. Trying next candidate...")
                    last_error = e

        logger.error(f"[OpenRouter LLM] All {len(candidate_models)} candidate models failed. Triggering template fallback.")
        raise RuntimeError(f"All OpenRouter candidate models exhausted: {last_error}")

    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        now = datetime.datetime.now()
        today_str = now.strftime('%d/%m/%Y')
        time_str = now.strftime('%H:%M:%S')

        # 1. Trích xuất ngữ cảnh RAG và danh mục phim đang chiếu để Grounding
        rag_context = ""
        try:
            kb_results = search_knowledge_base.invoke({"query": user_message})
            if kb_results and "không tìm thấy" not in kb_results.lower():
                rag_context += f"\n\n## Kiến thức chính sách & ưu đãi (RAG):\n{kb_results}"
        except Exception as e:
            logger.debug(f"RAG search error in LlmEngine: {e}")

        # 2. Xây dựng dynamic system prompt
        dynamic_system_prompt = (
            f"{SYSTEM_PROMPT}\n\n"
            f"## Ngữ cảnh hệ thống:\n"
            f"- Thời gian hiện tại: {time_str} ngày {today_str}\n"
            f"{rag_context}"
        )

        # 3. Tổng hợp messages với memory
        history = self._get_history(session_id)
        messages = [{"role": "system", "content": dynamic_system_prompt}]
        messages.extend(history)
        messages.append({"role": "user", "content": user_message})

        # 4. Gọi OpenRouter LLM
        reply = self._call_openrouter(messages)

        # 5. Lưu vết memory sau khi thành công
        self._append_history(session_id, "user", user_message)
        self._append_history(session_id, "assistant", reply)

        return reply
