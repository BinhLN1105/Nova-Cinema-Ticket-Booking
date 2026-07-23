class BaseChatEngine:
    def process(self, user_message: str, session_id: str, user_id: str = None) -> str:
        raise NotImplementedError("Each engine must implement the process method.")
