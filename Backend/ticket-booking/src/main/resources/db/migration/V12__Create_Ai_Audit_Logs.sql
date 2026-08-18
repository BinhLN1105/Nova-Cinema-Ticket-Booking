-- V12: Tạo bảng ai_audit_logs để lưu lịch sử tương tác với AI chatbot
CREATE TABLE IF NOT EXISTS ai_audit_logs (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL,
    session_id      VARCHAR(255)  NOT NULL,
    user_message    VARCHAR(2000),
    ai_response     VARCHAR(4000),
    intent          VARCHAR(50),
    used_fallback   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ai_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_ai_audit_logs_session ON ai_audit_logs(session_id);
CREATE INDEX idx_ai_audit_logs_created ON ai_audit_logs(created_at DESC);
