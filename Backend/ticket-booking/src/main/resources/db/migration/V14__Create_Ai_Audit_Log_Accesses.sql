-- V14: Tạo bảng ai_audit_log_accesses để ghi vết nhân viên truy cập nhật ký hội thoại AI (Audit the Auditor)
CREATE TABLE IF NOT EXISTS ai_audit_log_accesses (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    accessor_user_id    UUID          NOT NULL,
    accessor_role       VARCHAR(50)   NOT NULL,
    target_session_id   VARCHAR(255),
    target_user_id      UUID,
    ip_address          VARCHAR(100),
    reason              VARCHAR(500),
    accessed_at         TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ai_audit_log_accesses PRIMARY KEY (id)
);

CREATE INDEX idx_ai_audit_log_accesses_accessor ON ai_audit_log_accesses(accessor_user_id);
CREATE INDEX idx_ai_audit_log_accesses_target_session ON ai_audit_log_accesses(target_session_id);
CREATE INDEX idx_ai_audit_log_accesses_accessed ON ai_audit_log_accesses(accessed_at DESC);
