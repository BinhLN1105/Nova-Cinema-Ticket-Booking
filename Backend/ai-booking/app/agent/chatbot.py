"""
app/agent/chatbot.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AI Agent được trang bị tools.
Sử dụng LangChain + Gemini (hoặc GPT) với ReAct agent pattern.

Mỗi session_id có conversation history riêng (lưu trong RAM).
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.memory import ConversationBufferWindowMemory
from langchain.schema import HumanMessage, AIMessage

from ..config import get_settings
from .tools import ALL_TOOLS, search_knowledge_base, get_now_showing_movies

cfg = get_settings()

# ── System prompt — định nghĩa nhân cách và giới hạn của chatbot ─
SYSTEM_PROMPT = """Bạn là Nova — trợ lý AI thông minh của NovaTicket, ứng dụng đặt vé xem phim hàng đầu Việt Nam.

## Nhiệm vụ của bạn
Hỗ trợ khách hàng tra cứu và đặt vé xem phim một cách nhanh chóng, chính xác và thân thiện.

## Nguyên tắc bắt buộc
1. **Chỉ dùng tool để lấy thông tin** — KHÔNG bao giờ bịa ra số liệu về ghế, giờ chiếu, giá vé.
2. **Dùng đúng tool**:
   - Câu hỏi về chính sách, quy định, ưu đãi cố định → `search_knowledge_base`
   - Hỏi phim đang chiếu → `get_now_showing_movies`
   - Hỏi lịch chiếu cụ thể → `get_showtimes`
   - Hỏi ghế trống → `get_available_seats` (cần có showtime_id từ `get_showtimes`)
   - Hỏi voucher/khuyến mãi → `get_active_vouchers`
3. **Trả lời ngắn gọn, đúng trọng tâm** — không dài dòng, không lặp lại câu hỏi.
4. **Thân thiện, tự nhiên** — dùng tiếng Việt tự nhiên, xưng "em" và gọi khách là "anh/chị".
5. **Thừa nhận giới hạn** — nếu không tìm được thông tin, nói thật thay vì đoán mò.

## Ví dụ cách dùng tool kết hợp
Khi khách hỏi "Phim Mai 8h tối nay rạp Landmark còn ghế không?":
1. Gọi `get_showtimes(movie_title="Mai", cinema_name="Landmark 81", date="hôm nay")` để lấy showtime_id
2. Gọi `get_available_seats(showtime_id=...)` để lấy số ghế trống
3. Tổng hợp và trả lời

## Giới hạn của bạn
- Bạn KHÔNG thể đặt vé giúp khách (chỉ tra cứu thông tin, việc đặt vé làm trên app/web)
- Bạn KHÔNG biết thông tin cá nhân của khách (lịch sử đặt vé, điểm tích lũy cụ thể)
- Với câu hỏi ngoài phạm vi điện ảnh, lịch sự từ chối và hướng về chủ đề chính
"""

# ── Session Memory — mỗi session_id có history riêng ─────────
_session_memories: dict[str, ConversationBufferWindowMemory] = {}

def _get_memory(session_id: str) -> ConversationBufferWindowMemory:
    if session_id not in _session_memories:
        _session_memories[session_id] = ConversationBufferWindowMemory(
            memory_key="chat_history",
            return_messages=True,
            k=6            # Giữ 6 lượt cuối (3 hỏi + 3 đáp) để không spam token
        )
    return _session_memories[session_id]

def clear_session(session_id: str):
    """Xóa lịch sử chat của 1 session"""
    _session_memories.pop(session_id, None)


# ── Build LLM + Agent ─────────────────────────────────────────
def _build_llm():
    if cfg.gemini_api_key:
        return ChatGoogleGenerativeAI(
            model=cfg.llm_model,
            google_api_key=cfg.gemini_api_key,
            temperature=cfg.llm_temperature,
            # convert_system_message_to_human đã được tích hợp mặc định trong các ver mới
            # gỡ bỏ để tránh lỗi Tool calling
        )
    elif cfg.openai_api_key:
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(
            model=cfg.llm_model,
            openai_api_key=cfg.openai_api_key,
            temperature=cfg.llm_temperature,
            streaming=True
        )
    else:
        raise ValueError("Cần cấu hình GEMINI_API_KEY hoặc OPENAI_API_KEY trong .env")


def _build_agent_executor(memory: ConversationBufferWindowMemory) -> AgentExecutor:
    llm = _build_llm()

    prompt = ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    agent = create_tool_calling_agent(llm, ALL_TOOLS, prompt)

    return AgentExecutor(
        agent=agent,
        tools=ALL_TOOLS,
        memory=memory,
        verbose=True,           # Bật để xem agent suy nghĩ trong log
        max_iterations=5,       # Tối đa 5 bước tool calling / câu hỏi
        handle_parsing_errors=True,
        return_intermediate_steps=False,
    )


# ── Public interface ──────────────────────────────────────────
def chat(session_id: str, user_message: str) -> str:
    """
    Điểm vào duy nhất — nhận câu hỏi, trả về câu trả lời.
    Agent tự quyết định dùng tool nào dựa vào nội dung câu hỏi.
    """
    memory   = _get_memory(session_id)
    executor = _build_agent_executor(memory)

    try:
        result = executor.invoke({"input": user_message})
        return result["output"]
    except Exception as e:
        error_str = str(e)
        import logging
        logging.error(f"Agent error (session={session_id}): {e}")

        # Kiểm tra nếu là lỗi giới hạn Quota (429 / ResourceExhausted)
        if any(exc in error_str for exc in ["429", "Quota exceeded", "ResourceExhausted", "ResourceExhausted"]):
            return _handle_quota_exhausted_fallback(user_message)

        # Fallback an toàn cho các lỗi khác
        return (
            "Em xin lỗi anh/chị, hiện em đang gặp sự cố kỹ thuật. "
            "Anh/chị vui lòng thử lại sau hoặc liên hệ hotline 1900 6789 để được hỗ trợ ạ."
        )

def _handle_quota_exhausted_fallback(user_message: str) -> str:
    """
    Xử lý khi LLM hết quota: Tự gọi tool RAG thô để trả về thông tin cho khách.
    """
    
    parts = [
        "⚠️ **Hệ thống AI đang tạm thời quá tải (Quota Limit)**",
        "Em xin lỗi vì sự bất tiện này. Dưới đây là thông tin em tìm được trực tiếp từ cơ sở dữ liệu cho anh/chị:",
    ]
    
    # 1. Tra cứu Knowledge Base (RAG)
    try:
        # invoke tool thủ công
        rag_info = search_knowledge_base.invoke(user_message)
        if "Không tìm thấy" not in rag_info and "Lỗi" not in rag_info:
            parts.append("\n**📍 Thông tin từ cơ sở kiến thức:**\n" + rag_info)
    except Exception:
        pass
    
    # 2. Nếu hỏi về phim đang chiếu thì lấy list phim
    msg_lower = user_message.lower()
    movie_keywords = ["phim", "chiếu", "xem", "lịch", "phim gì", "đang chiếu"]
    if any(k in msg_lower for k in movie_keywords):
        try:
            movies = get_now_showing_movies.invoke("")
            if "Lỗi" not in movies:
                parts.append("\n**🎬 Danh sách phim đang chiếu tại rạp:**\n" + movies)
        except Exception:
            pass

    # Nếu không tìm thấy gì cả
    if len(parts) <= 2:
        return (
            "Em xin lỗi, hiện tại hệ thống AI đang quá tải và em cũng không tìm thấy thông tin khớp trực tiếp với yêu cầu của anh/chị. "
            "Anh/chị vui lòng thử lại sau ít phút hoặc truy cập website [novaticket.com](https://novaticket.com) để tra cứu nhé!"
        )

    parts.append("\n_Vì AI đang quá tải, câu trả lời này được trích xuất tự động và chưa qua xử lý ngôn ngữ. Mong anh/chị thông cảm!_")
    return "\n".join(parts)

