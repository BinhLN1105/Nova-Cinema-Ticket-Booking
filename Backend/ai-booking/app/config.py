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
    gemini_api_key:  str = ""
    openai_api_key:  str = ""
    llm_model:       str = "gemini-2.5-flash"
    llm_temperature: float = 0.1

    # ChromaDB
    chroma_persist_dir: str = "./chroma_db"
    chroma_collection:  str = "nova_knowledge"

    # Embedding
    embedding_model: str = "intfloat/multilingual-e5-base"

    # Chunking
    chunk_size:    int = 400
    chunk_overlap: int = 50

    # Security
    jwt_secret: str = ""
    cors_origins: str = "http://localhost:8080,http://localhost:3000"

    class Config:
        env_file = ".env"
        extra = "ignore"

@lru_cache
def get_settings() -> Settings:
    return Settings()
