"""
app/agent/chatbot.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AI Agent & Smart Template Engine cho NovaTicket Chatbot.

Hỗ trợ 2 chế độ:
  1. Smart Template + Local RAG Engine (mặc định / USE_MOCK_AI=true):
     Trả lời cực nhanh, chuẩn xác 100%, không tốn API key.
  2. OpenRouter LLM Engine (khi USE_MOCK_AI=false và có OPENROUTER_API_KEY):
     Hỗ trợ các model AI tiên tiến, nhiệt độ 0.0 chống hallucination,
     kèm cơ chế tự động Fallback về Template khi gặp Rate Limit (429) hoặc lỗi mạng.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

import datetime
import logging
from ..config import get_settings
from .agent_factory import AgentFactory

logger = logging.getLogger(__name__)
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
        
        # Override intent statefully nếu đang trong luồng đặt vé và nhắc lịch
        from .state import session_manager
        state = session_manager.get_state(session_id)
        from .intent_classifier import remove_vietnamese_accents
        msg_clean = remove_vietnamese_accents(user_message.lower())
        
        # Danh sách các bước trong luồng nhắc lịch
        reminder_steps = [
            "select_reminder_type", 
            "select_reminder_flow", 
            "booking_flow_select_movie", 
            "booking_flow_select_cinema", 
            "showtime_flow_select_ticket"
        ]
        
        # Nếu đang ở clarify_movie của luồng nhắc lịch
        is_clarify_reminder = (
            state.get("current_step") == "clarify_movie" and 
            state.get("next_step") == "booking_flow_select_cinema"
        )
        
        is_in_reminder_flow = (
            state.get("current_step") in reminder_steps or 
            state.get("awaiting_reminder_showtime") is True or
            is_clarify_reminder
        )
        
        # Nếu đang ở clarify_movie của luồng đặt vé
        is_clarify_booking = (
            state.get("current_step") == "clarify_movie" and 
            state.get("next_step") == "booking_flow_search"
        )
        
        if is_in_reminder_flow and not any(x in msg_clean for x in ["dat ve", "mua ve", "dat ghe", "giu ghe"]):
            intent = "REMINDER_DRAFT"
        elif is_clarify_booking:
            intent = "BOOKING_DRAFT"
        elif intent in ["UNKNOWN", "KNOWLEDGE_RAG"] and (
            state.get("showtime_id") is not None 
            or state.get("awaiting_movie") is True
            or state.get("showtime_list")
        ):
            has_rag_keywords = any(x in msg_clean for x in ["hoan ve", "huy ve", "chinh sach", "gia ve", "bap nuoc", "combo", "vnpay", "thanh toan", "lien he", "dia chi", "lich su", "diem", "point", "cinepoint", "ve da mua", "da dat", "ve cua toi", "the", "rank", "sao", "nhu nao", "huong dan", "quy dinh"])
            if not has_rag_keywords:
                intent = "BOOKING_DRAFT"
    except Exception:
        intent = "UNKNOWN"

    used_fallback = False
    reply_text = ""
    use_mock = getattr(cfg, 'use_mock_ai', True)

    # Danh mục Intent nghiệp vụ giao tác cần Engine xử lý trực tiếp (State Machine + Java Backend API)
    transactional_intents = {"BOOKING_DRAFT", "REMINDER_DRAFT", "REMINDER_SCHEDULE", "USER_QUERIES", "NOW_SHOWING"}
    is_transactional = (intent in transactional_intents) or is_in_reminder_flow or is_clarify_booking or (
        state.get("showtime_id") is not None or state.get("awaiting_movie") is True or state.get("showtime_list")
    )
    is_testing_llm_fallback = "trigger_ratelimit_429" in user_message or "trigger_server_error_500" in user_message

    try:
        # 1. Nếu hệ thống ép fallback / mock, HOẶC là tác vụ nghiệp vụ giao tác (Đặt vé nháp, Nhắc lịch, Xem điểm/vé)
        if (not is_testing_llm_fallback) and (force_fallback or use_mock or is_transactional):
            used_fallback = bool(force_fallback or use_mock)
            engine = AgentFactory.get_engine(force_fallback=True)
            reply_text = engine.process(user_message, session_id, user_id=user_id)
        else:
            # 2. Nếu là câu hỏi tư vấn, chào hỏi, chính sách, kiến thức mở -> Sử dụng LLM Engine (DeepSeek / OpenRouter)
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

