from .engines import TemplateEngine, LlmEngine
from ..config import get_settings

cfg = get_settings()

class AgentFactory:
    _template_instance = None
    _llm_instance = None

    @classmethod
    def get_engine(cls, force_fallback: bool = False):
        """
        Quyết định Engine nào được sử dụng dựa trên biến môi trường và API Keys.
        Tự động fallback về TemplateEngine nếu không cấu hình API Key.
        """
        use_mock = getattr(cfg, 'use_mock_ai', True)
        
        # Bắt buộc phải có khóa nếu muốn dùng LLM
        has_api_keys = bool(cfg.gemini_api_key or cfg.openai_api_key)

        if force_fallback or use_mock or not has_api_keys:
            if cls._template_instance is None:
                cls._template_instance = TemplateEngine()
            return cls._template_instance
        
        # Trả về LLM Engine thực tế
        if cls._llm_instance is None:
            try:
                cls._llm_instance = LlmEngine()
            except Exception as e:
                # Nếu khởi tạo LLM lỗi (lỗi nạp thư viện...), tự động fallback
                import logging
                logging.error(f"Lỗi khởi tạo LlmEngine, tự động hạ cấp xuống TemplateEngine: {e}")
                if cls._template_instance is None:
                    cls._template_instance = TemplateEngine()
                return cls._template_instance
                
        return cls._llm_instance
