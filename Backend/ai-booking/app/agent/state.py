import json
import logging
from ..config import get_settings

cfg = get_settings()

class SessionStateManager:
    def __init__(self):
        self.use_redis = False
        self.redis_client = None
        
        # Thử khởi tạo kết nối Upstash Redis/Standard Redis nếu cấu hình tồn tại
        redis_host = getattr(cfg, 'redis_host', None) or "localhost"
        redis_port = getattr(cfg, 'redis_port', None) or 6379
        
        if getattr(cfg, 'upstash_redis_url', None):
            try:
                # Cách 1: Thử thư viện Upstash chuyên dụng qua HTTP (phù hợp Serverless)
                from upstash_redis import Redis
                token = getattr(cfg, 'upstash_redis_token', '')
                self.redis_client = Redis(url=cfg.upstash_redis_url, token=token)
                self.use_redis = True
                print("⚡ [Redis] Đăng ký thành công Upstash Redis cho AI memory.")
            except ImportError:
                # Cách 2: Thử sử dụng thư viện redis chuẩn TCP
                try:
                    import redis
                    # Upstash hỗ trợ giao thức redis:// rediss://
                    self.redis_client = redis.from_url(cfg.upstash_redis_url)
                    self.use_redis = True
                    print("⚡ [Redis] Đăng ký thành công Upstash qua thư viện redis TCP.")
                except Exception as e:
                    logging.warning(f"Chưa có thư viện redis, bộ nhớ AI lưu cục bộ: {str(e)}")
        
        self.local_mem = {}

    def get_state(self, session_id: str) -> dict:
        if self.use_redis and self.redis_client:
            try:
                if hasattr(self.redis_client, 'get'):
                    data = self.redis_client.get(f"ai_state:{session_id}")
                    if data:
                        # Decode nếu là bytes (redis TCP)
                        if isinstance(data, bytes):
                            data = data.decode('utf-8')
                        return json.loads(data)
            except Exception as e:
                logging.error(f"Lỗi đọc state từ Redis: {e}")
        return self.local_mem.get(session_id, {})

    def set_state(self, session_id: str, state: dict, ttl: int = 1200):
        if self.use_redis and self.redis_client:
            try:
                # Hỗ trợ set có expire của upstash (phương thức tùy thuộc thư viện)
                val_json = json.dumps(state)
                if hasattr(self.redis_client, 'set'):
                    self.redis_client.set(f"ai_state:{session_id}", val_json, ex=ttl)
                    return
            except Exception as e:
                logging.error(f"Lỗi ghi state lên Redis: {e}")
                
        self.local_mem[session_id] = state

    def clear(self, session_id: str):
        if self.use_redis and self.redis_client:
            try:
                if hasattr(self.redis_client, 'delete'):
                    self.redis_client.delete(f"ai_state:{session_id}")
                    return
            except Exception as e:
                logging.error(f"Lỗi xóa state trên Redis: {e}")
        self.local_mem.pop(session_id, None)

session_manager = SessionStateManager()
