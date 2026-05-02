-- ── ADD USER_HIDDEN_NOTIFICATIONS TABLE ──────────────────────────────
CREATE TABLE IF NOT EXISTS user_hidden_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    global_notification_id UUID NOT NULL REFERENCES global_notifications(id),
    hidden_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_hidden_notification UNIQUE (user_id, global_notification_id)
);

CREATE INDEX IF NOT EXISTS idx_user_hidden_notif_user ON user_hidden_notifications(user_id);
