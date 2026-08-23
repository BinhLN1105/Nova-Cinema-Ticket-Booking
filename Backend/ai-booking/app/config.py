from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    # Server
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    app_env:  str = "development"

    # Java API
    java_api_base:    str = "http://localhost:8080"
    internal_api_key: str = ""

    # LLM
    openrouter_api_key:  str = ""
    openrouter_base_url: str = "https://openrouter.ai/api/v1"
    gemini_api_key:      str = ""
    openai_api_key:      str = ""
    cohere_api_key:      str = ""
    llm_model:           str = "meta-llama/llama-3.3-70b-instruct:free"
    llm_fallback_models: str = "google/gemini-2.0-flash-exp:free,deepseek/deepseek-chat,mistralai/mistral-7b-instruct:free,qwen/qwen-2.5-72b-instruct:free"
    llm_temperature:     float = 0.0  # Nhiệt độ thấp (0.0) chuẩn enterprise giúp AI không bịa đặt
    llm_timeout:         float = 8.0  # Thời gian chờ tối đa (giây) cho mỗi model
    llm_max_tokens:      int = 1024   # Độ dài token tối đa cho câu trả lời
    use_mock_ai:         bool = True  # True: ưu tiên offline smart template; False: gọi OpenRouter LLM

    # Vector DB (FAISS)
    vector_db_dir:     str = "./faiss_index"
    chroma_collection:  str = "nova_knowledge"

    # Embedding
    embedding_model: str = "embed-multilingual-v3.0"

    # Chunking
    chunk_size:    int = 400
    chunk_overlap: int = 50

    # Security
    jwt_secret: str = ""
    cors_origins: str = "http://localhost:8080"

    class Config:
        env_file = ".env"
        extra = "ignore"

@lru_cache
def get_settings() -> Settings:
    return Settings()
