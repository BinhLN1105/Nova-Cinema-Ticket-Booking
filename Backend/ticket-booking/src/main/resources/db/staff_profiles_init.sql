-- ============================================================
-- staff_profiles DDL — Production Reference Script
-- (Dev: Hibernate tự tạo bảng này qua ddl-auto=update)
-- ============================================================

CREATE TABLE IF NOT EXISTS staff_profiles (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    cinema_id  UUID         NOT NULL,
    employee_code VARCHAR(50),
    created_at TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_staff_profiles     PRIMARY KEY (id),

    -- Mỗi User chỉ có tối đa 1 StaffProfile (OneToOne)
    CONSTRAINT uq_staff_profiles_user UNIQUE (user_id),

    -- FK → users: khi xóa User thì xóa luôn StaffProfile
    CONSTRAINT fk_staff_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    -- FK → cinemas: không cho phép xóa Cinema khi còn Staff thuộc rạp đó
    CONSTRAINT fk_staff_profiles_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id)
        ON DELETE RESTRICT
);

-- Index cho FK cinema_id (tra cứu "danh sách staff của rạp X")
CREATE INDEX IF NOT EXISTS idx_staff_profiles_cinema_id
    ON staff_profiles (cinema_id);
