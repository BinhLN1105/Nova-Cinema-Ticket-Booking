"""
app/agent/chatbot.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AI Agent & Smart Template Engine cho NovaTicket Chatbot.

Hỗ trợ 2 chế độ:
  1. Smart Template + Local RAG Engine (mặc định / USE_MOCK_AI=true):
     Trả lời cực nhanh, không tốn API key, không sợ rate limit Gemini (429),
     vẫn hỗ trợ tra cứu RAG (FAISS index) và gọi các Tool nội bộ khi có Java server.
  2. Gemini/LangChain ReAct Agent (khi USE_MOCK_AI=false và có API Key).
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

import datetime
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.memory import ConversationBufferWindowMemory

from ..config import get_settings
from .tools import ALL_TOOLS, search_knowledge_base, get_now_showing_movies, get_active_vouchers, get_showtimes, get_available_seats

cfg = get_settings()

SYSTEM_PROMPT = """Bạn là Nova — trợ lý AI thông minh của NovaTicket, ứng dụng đặt vé xem phim hàng đầu Việt Nam.

## Nhiệm vụ của bạn
Hỗ trợ khách hàng tra cứu và đặt vé xem phim một cách nhanh chóng, chính xác và thân thiện.

## Nguyên tắc bắt buộc
1. Chỉ dùng tool để lấy thông tin — KHÔNG bao giờ bịa ra số liệu về ghế, giờ chiếu, giá vé.
2. Dùng đúng tool:
   - Câu hỏi về chính sách, quy định, ưu đãi cố định → search_knowledge_base
   - Hỏi phim đang chiếu → get_now_showing_movies
   - Hỏi lịch chiếu cụ thể → get_showtimes
   - Hỏi ghế trống → get_available_seats (cần có showtime_id từ get_showtimes)
   - Hỏi voucher/khuyến mãi → get_active_vouchers
3. Trả lời ngắn gọn, đúng trọng tâm — không dài dòng, không lặp lại câu hỏi.
4. YÊU CẦU ĐỊNH DẠNG TUYỆT ĐỐI (PLAIN TEXT CHUẨN): Ứng dụng di động không hỗ trợ Markdown.
   - TUYỆT ĐỐI KHÔNG dùng dấu sao (*) để in đậm, in nghiêng hay làm gạch đầu dòng.
   - TUYỆT ĐỐI KHÔNG dùng dấu thăng (#) cho tiêu đề.
   - Hãy dùng dấu gạch ngang (-) hoặc đánh số (1, 2, 3) để liệt kê.
5. Thân thiện, tự nhiên — dùng tiếng Việt tự nhiên, xưng "em" và gọi khách là "anh/chị".
"""

import datetime
from ..config import get_settings
from .agent_factory import AgentFactory

cfg = get_settings()

def clear_session(session_id: str):
    """Xóa bộ nhớ đệm trạng thái của session trong memory/Redis"""
    from .state import session_manager
    session_manager.clear(session_id)

# ── Public interface ──────────────────────────────────────────
def chat(session_id: str, user_message: str, user_id: str = None, force_fallback: bool = False) -> dict:
    """
    Điểm vào duy nhất cho Chatbot.
    Hỗ trợ 2-way fallback và phân loại intent.
    """
    from .intent_classifier import IntentClassifier
    
    # 1. Phân loại ý định trước để trả về Java lưu vào database kiểm toán
    try:
        classifier = IntentClassifier()
        intent = classifier.classify(user_message)
        
        # Override intent statefully nếu đang trong luồng đặt vé và tin nhắn chứa thông tin phụ
        from .state import session_manager
        state = session_manager.get_state(session_id)
        if intent in ["KNOWLEDGE_RAG", "UNKNOWN"] and (state.get("showtime_id") is not None or state.get("awaiting_movie") is True):
            intent = "BOOKING_DRAFT"
    except Exception:
        intent = "UNKNOWN"

    used_fallback = False
    reply_text = ""
    use_mock = getattr(cfg, 'use_mock_ai', True)

    try:
        # Nếu Java chỉ định hạ cấp (do quá quota) hoặc config bắt buộc mock
        if force_fallback or use_mock:
            used_fallback = True
            engine = AgentFactory.get_engine(force_fallback=True)
            reply_text = engine.process(user_message, session_id, user_id=user_id)
        else:
            try:
                engine = AgentFactory.get_engine(force_fallback=False)
                reply_text = engine.process(user_message, session_id, user_id=user_id)
            except Exception as e:
                import logging
                logging.error(f"[Chatbot LLM Fallback] Lỗi API LLM: {str(e)}. Tự động hạ xuống Offline Template.")
                used_fallback = True
                engine = AgentFactory.get_engine(force_fallback=True)
                reply_text = engine.process(user_message, session_id, user_id=user_id)
    except Exception as e:
        import traceback
        traceback.print_exc()
        import logging
        logging.critical(f"Critical execution error in chatbot.chat: {e}")
        reply_text = "Xin lỗi anh/chị, hệ thống hỗ trợ AI đang gặp gián đoạn tạm thời. Vui lòng thử lại sau nhé!"
        used_fallback = True

    return {
        "reply": reply_text,
        "intent": intent,
        "used_fallback": used_fallback
    }

def get_smart_template_response(user_message: str) -> str:
    """Chức năng tương thích ngược cho template phản hồi"""
    from .engines import TemplateEngine
    try:
        return TemplateEngine().process(user_message, "default_session", None)
    except Exception:
        return "Dạ, em chưa tìm thấy thông tin cần thiết. Anh/chị vui lòng gõ câu hỏi khác nhé!"

