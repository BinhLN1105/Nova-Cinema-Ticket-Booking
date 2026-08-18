import datetime
from .base_engine import BaseChatEngine
from ...config import get_settings
from ...agent.tools import ALL_TOOLS
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.memory import ConversationBufferWindowMemory

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
   - Hỏi ghế trống → get_available_seats
   - Hỏi voucher/khuyến mãi → get_active_vouchers
3. Trả lời ngắn gọn, đúng trọng tâm — không dài dòng, không lặp lại câu hỏi.
4. YÊU CẦU ĐỊNH DẠNG TUYỆT ĐỐI (PLAIN TEXT CHUẨN): Ứng dụng di động không hỗ trợ Markdown.
   - TUYỆT ĐỐI KHÔNG dùng dấu sao (*) để in đậm, in nghiêng hay làm gạch đầu dòng.
   - TUYỆT ĐỐI KHÔNG dùng dấu thăng (#) cho tiêu đề.
   - Hãy dùng dấu gạch ngang (-) hoặc đánh số (1, 2, 3) để liệt kê.
5. Thân thiện, tự nhiên — dùng tiếng Việt tự nhiên, xưng "em" và gọi khách là "anh/chị".
"""

class LlmEngine(BaseChatEngine):
    def __init__(self):
        self._session_memories = {}

    def _get_memory(self, session_id: str) -> ConversationBufferWindowMemory:
        if session_id not in self._session_memories:
            self._session_memories[session_id] = ConversationBufferWindowMemory(
                memory_key="chat_history",
                return_messages=True,
                k=6
            )
        return self._session_memories[session_id]

    def _build_llm(self):
        if cfg.gemini_api_key:
            return ChatGoogleGenerativeAI(
                model=cfg.llm_model,
                google_api_key=cfg.gemini_api_key,
                temperature=cfg.llm_temperature,
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
            raise ValueError("Cần cấu hình GEMINI_API_KEY hoặc OPENAI_API_KEY")

    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        memory = self._get_memory(session_id)
        llm = self._build_llm()
        
        now = datetime.datetime.now()
        today_str = now.strftime('%d/%m/%Y')
        tomorrow_str = (now + datetime.timedelta(days=1)).strftime('%d/%m/%Y')
        time_str = now.strftime('%H:%M:%S')

        dynamic_system_prompt = SYSTEM_PROMPT + f"\n\n## Ngữ cảnh thời gian:\n- Hôm nay: {today_str}\n- Ngày mai: {tomorrow_str}\n- Giờ: {time_str}\n"

        prompt = ChatPromptTemplate.from_messages([
            ("system", dynamic_system_prompt),
            MessagesPlaceholder(variable_name="chat_history"),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ])

        agent = create_tool_calling_agent(llm, ALL_TOOLS, prompt)
        executor = AgentExecutor(
            agent=agent,
            tools=ALL_TOOLS,
            memory=memory,
            verbose=True,
            max_iterations=5,
            handle_parsing_errors=True,
            return_intermediate_steps=False,
        )
        
        result = executor.invoke({"input": user_message})
        return result["output"]
