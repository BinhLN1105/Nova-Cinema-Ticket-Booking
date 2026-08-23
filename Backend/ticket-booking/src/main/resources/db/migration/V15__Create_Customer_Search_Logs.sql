-- V15: Tạo bảng customer_search_logs để ghi vết nhân viên CSKH/Admin tìm kiếm thông tin khách hàng
CREATE TABLE IF NOT EXISTS customer_search_logs (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    accessor_user_id    UUID          NOT NULL,
    accessor_role       VARCHAR(50)   NOT NULL,
    search_query        VARCHAR(255)  NOT NULL,
    result_count        INTEGER       NOT NULL DEFAULT 0,
    ip_address          VARCHAR(100),
    searched_at         TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_customer_search_logs PRIMARY KEY (id)
);

CREATE INDEX idx_customer_search_logs_accessor ON customer_search_logs(accessor_user_id);
CREATE INDEX idx_customer_search_logs_searched ON customer_search_logs(searched_at DESC);
