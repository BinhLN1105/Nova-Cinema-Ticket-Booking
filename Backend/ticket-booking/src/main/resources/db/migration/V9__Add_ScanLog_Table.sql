-- V9: Tạo bảng scan_logs để lưu lịch sử soát vé (thành công + thất bại)
CREATE TABLE IF NOT EXISTS scan_logs (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    staff_id        UUID         NOT NULL,
    cinema_id       UUID         NOT NULL,
    booking_id      UUID,                         -- null nếu QR hoàn toàn không hợp lệ
    qr_code_raw     TEXT,
    success         BOOLEAN      NOT NULL DEFAULT FALSE,
    fail_reason     VARCHAR(200),
    movie_title     VARCHAR(200),
    movie_poster_url TEXT,
    customer_name   VARCHAR(100),
    customer_phone  VARCHAR(20),
    seats_checked   VARCHAR(100),
    screen_name     VARCHAR(50),
    scanned_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_scan_logs PRIMARY KEY (id),
    CONSTRAINT fk_scan_logs_staff   FOREIGN KEY (staff_id)   REFERENCES users(id),
    CONSTRAINT fk_scan_logs_cinema  FOREIGN KEY (cinema_id)  REFERENCES cinemas(id),
    CONSTRAINT fk_scan_logs_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL
);

CREATE INDEX idx_scan_logs_cinema_scanned ON scan_logs(cinema_id, scanned_at DESC);
CREATE INDEX idx_scan_logs_staff_scanned  ON scan_logs(staff_id, scanned_at DESC);
