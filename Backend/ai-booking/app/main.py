"""
app/main.py
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FastAPI server — chỉ mở đúng những API cần thiết.

Endpoints:
  POST /api/v1/chat       ← Java gọi vào đây
  POST /api/v1/sync       ← Admin trigger re-ingest (internal)
  GET  /health            ← Health check
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

from fastapi import FastAPI, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import logging, subprocess, sys

from .config import get_settings
from .agent.chatbot import chat as agent_chat, clear_session

cfg = get_settings()
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("nova-rag")

app = FastAPI(
    title="NovaTicket RAG Chatbot API",
    version="1.0.0",
    docs_url="/docs" if cfg.app_env == "development" else None,  # Ẩn docs trên prod
)

# CORS — đọc từ biến môi trường CORS_ORIGINS (phân cách bằng dấu phẩy)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in cfg.cors_origins.split(",")],
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


# ── Security dependency ───────────────────────────────────────
def verify_internal_key(x_internal_key: str = Header(...)) -> None:
    """Bảo vệ endpoint /sync — chỉ Java server mới được gọi"""
    if x_internal_key != cfg.internal_api_key:
        raise HTTPException(status_code=403, detail="Unauthorized")


# ════════════════════════════════════════════════════════════
#  POST /api/v1/chat  — endpoint chính
# ════════════════════════════════════════════════════════════
class ChatRequest(BaseModel):
    session_id:   str
    user_message: str

class ChatResponse(BaseModel):
    reply:      str
    session_id: str

@app.post("/api/v1/chat", response_model=ChatResponse)
async def chat_endpoint(req: ChatRequest):
    """
    Nhận câu hỏi từ Java → Agent xử lý → trả câu trả lời.
    Java gọi endpoint này sau khi nhận tin nhắn từ frontend.
    """
    if not req.user_message.strip():
        raise HTTPException(status_code=400, detail="user_message không được để trống")

    logger.info(f"[Chat] session={req.session_id} | msg={req.user_message[:80]}")

    reply = agent_chat(
        session_id=req.session_id,
        user_message=req.user_message
    )

    logger.info(f"[Chat] session={req.session_id} | reply={reply[:80]}")
    return ChatResponse(reply=reply, session_id=req.session_id)


# ════════════════════════════════════════════════════════════
#  POST /api/v1/sync  — trigger re-ingest file tĩnh
# ════════════════════════════════════════════════════════════
class SyncResponse(BaseModel):
    status:  str
    message: str

@app.post("/api/v1/sync", response_model=SyncResponse,
          dependencies=[Depends(verify_internal_key)])
async def sync_endpoint():
    """
    Admin/Java trigger để re-ingest toàn bộ file tĩnh vào ChromaDB.
    Dùng khi team cập nhật chính sách, FAQ.
    Bảo vệ bằng X-Internal-Key header.
    """
    try:
        logger.info("[Sync] Starting re-ingestion...")
        # Chạy ingest script trong subprocess để không block server
        result = subprocess.run(
            [sys.executable, "scripts/ingest.py"],
            capture_output=True, text=True, timeout=300
        )
        if result.returncode == 0:
            logger.info("[Sync] Done.")
            return SyncResponse(status="success",
                                message="Đã nạp lại toàn bộ dữ liệu vào Vector DB")
        else:
            logger.error(f"[Sync] Error: {result.stderr}")
            return SyncResponse(status="error", message=result.stderr[:200])
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ════════════════════════════════════════════════════════════
#  POST /api/v1/session/clear  — xóa lịch sử chat
# ════════════════════════════════════════════════════════════
@app.post("/api/v1/session/clear")
async def clear_session_endpoint(session_id: str):
    clear_session(session_id)
    return {"status": "ok", "message": f"Đã xóa lịch sử session {session_id}"}


# ════════════════════════════════════════════════════════════
#  GET /health  — health check
# ════════════════════════════════════════════════════════════
@app.get("/health")
async def health():
    from .ingestion.vector_store import VectorStore
    from .ingestion.embedder import EmbeddingModel

    try:
        store = VectorStore(
            EmbeddingModel(cfg.embedding_model),
            persist_dir=cfg.chroma_persist_dir,
            collection_name=cfg.chroma_collection
        )
        chunk_count = store.count()
        vector_db_ok = True
    except Exception:
        chunk_count = 0
        vector_db_ok = False

    return {
        "status":       "ok" if vector_db_ok else "degraded",
        "vector_db":    "ok" if vector_db_ok else "error",
        "chunk_count":  chunk_count,
        "llm_model":    cfg.llm_model,
        "embedding":    cfg.embedding_model,
    }


# ── Dev run ───────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=cfg.app_host,
                port=cfg.app_port, reload=True)
