-- ── EXTENSIONS ────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS vector;

-- ── USERS ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN DEFAULT TRUE,
    reward_points BIGINT NOT NULL DEFAULT 0,
    available_exp BIGINT NOT NULL DEFAULT 0,
    membership_tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    fcm_token TEXT,
    allow_marketing_notification BOOLEAN NOT NULL DEFAULT TRUE,
    allow_transaction_notification BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- ── CINEMAS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cinemas (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);

-- ── SCREENS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS screens (
    id UUID PRIMARY KEY,
    cinema_id UUID NOT NULL REFERENCES cinemas(id),
    name VARCHAR(50) NOT NULL,
    screen_type VARCHAR(20) NOT NULL,
    total_rows INTEGER NOT NULL,
    total_cols INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ── SEATS ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS seats (
    id UUID PRIMARY KEY,
    screen_id UUID NOT NULL REFERENCES screens(id),
    row_label CHAR(1) NOT NULL,
    col_number INTEGER NOT NULL,
    grid_row INTEGER NOT NULL DEFAULT 0,
    grid_col INTEGER NOT NULL DEFAULT 0,
    seat_label VARCHAR(10) NOT NULL DEFAULT '',
    seat_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_seat_screen_row_col UNIQUE (screen_id, row_label, col_number),
    CONSTRAINT uk_seat_screen_grid UNIQUE (screen_id, grid_row, grid_col)
);

-- ── GENRES ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE
);

-- ── MOVIES ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS movies (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    original_title VARCHAR(200),
    description TEXT,
    duration INTEGER NOT NULL,
    release_date DATE NOT NULL,
    end_date DATE,
    director VARCHAR(150),
    movie_cast TEXT,
    language VARCHAR(50) DEFAULT 'Vietnamese',
    rated VARCHAR(10),
    poster_url TEXT,
    backdrop_url TEXT,
    trailer_url TEXT,
    avg_rating DECIMAL(3,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP
);

-- ── MOVIE_GENRES (Join Table) ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id UUID NOT NULL REFERENCES movies(id),
    genre_id INTEGER NOT NULL REFERENCES genres(id),
    PRIMARY KEY (movie_id, genre_id)
);

-- ── MOVIE_EMBEDDINGS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS movie_embeddings (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL UNIQUE REFERENCES movies(id),
    embedding VECTOR(768) NOT NULL,
    model_name VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP
);

-- ── SHOWTIMES ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS showtimes (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL REFERENCES movies(id),
    screen_id UUID NOT NULL REFERENCES screens(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_showtime_status_end ON showtimes (status, end_time);
CREATE INDEX IF NOT EXISTS idx_showtime_start_time ON showtimes (start_time);

-- ── SHOWTIME_SEATS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS showtime_seats (
    id UUID PRIMARY KEY,
    showtime_id UUID NOT NULL REFERENCES showtimes(id),
    seat_id UUID NOT NULL REFERENCES seats(id),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_by UUID REFERENCES users(id),
    locked_until TIMESTAMP,
    price DECIMAL(12,2) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_showtime_seat UNIQUE (showtime_id, seat_id)
);

-- ── VOUCHERS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vouchers (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    max_discount DECIMAL(12,2),
    min_order DECIMAL(12,2) DEFAULT 0,
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    applicable_to VARCHAR(30) NOT NULL DEFAULT 'ALL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);

-- ── USER_VOUCHERS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_vouchers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    voucher_id UUID NOT NULL REFERENCES vouchers(id),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    saved_at TIMESTAMP,
    CONSTRAINT uk_user_voucher UNIQUE (user_id, voucher_id)
);

-- ── BOOKINGS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    cinema_id UUID NOT NULL REFERENCES cinemas(id),
    showtime_id UUID REFERENCES showtimes(id),
    booking_code VARCHAR(20) NOT NULL UNIQUE,
    voucher_id UUID REFERENCES vouchers(id),
    discount_amount DECIMAL(12,2) DEFAULT 0,
    promotion_discount_amount DECIMAL(12,2) DEFAULT 0,
    applied_promotion_name VARCHAR(100),
    qr_code TEXT UNIQUE,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
    processed_by_id UUID REFERENCES users(id),
    note TEXT,
    earned_exp BIGINT DEFAULT 0,
    exp_added BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    cancellation_token VARCHAR(100),
    cancellation_token_expiry TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- ── COMBOS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS combos (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    image_url TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    type VARCHAR(20) DEFAULT 'COMBO'
);

-- ── BOOKING_COMBOS ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking_combos (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    combo_id UUID NOT NULL REFERENCES combos(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL
);

-- ── BOOKING_ITEMS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking_items (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    showtime_seat_id UUID NOT NULL REFERENCES showtime_seats(id),
    seat_price DECIMAL(12,2) NOT NULL
);

-- ── TICKETS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    booking_item_id UUID NOT NULL UNIQUE REFERENCES booking_items(id),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    issued_at TIMESTAMP
);

-- ── PAYMENTS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    amount DECIMAL(12,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    vnpay_txn_ref VARCHAR(100),
    vnpay_response_code VARCHAR(10),
    vnpay_bank_code VARCHAR(20),
    paid_at TIMESTAMP,
    created_at TIMESTAMP
);

-- ── TRANSACTIONS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference_id VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP
);

-- ── REVIEWS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    movie_id UUID NOT NULL REFERENCES movies(id),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    rating INTEGER NOT NULL,
    comment TEXT,
    is_visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    CONSTRAINT uk_review_user_movie UNIQUE (user_id, movie_id)
);

-- ── PROMOTIONS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    target_url VARCHAR(255),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_popup BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);

-- ── GIFT_CARDS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gift_cards (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(12,2) NOT NULL,
    point_value BIGINT NOT NULL,
    is_redeemed BOOLEAN NOT NULL DEFAULT FALSE,
    bought_by UUID REFERENCES users(id),
    redeemed_by UUID REFERENCES users(id),
    redeemed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP
);

-- ── GLOBAL_NOTIFICATIONS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS global_notifications (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_id UUID,
    target_topic VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP,
    sent_at TIMESTAMP
);

-- ── NOTIFICATIONS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    ref_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP
);

-- ── NOTIFICATION_CAMPAIGNS ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_campaigns (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_id UUID,
    target_topic VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by_id UUID REFERENCES users(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ── PASSWORD_RESET_TOKENS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP
);

-- ── PRICING_RULES ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pricing_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(255) NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    adjustment_type VARCHAR(255) NOT NULL,
    adjustment_value DECIMAL(12,2) NOT NULL,
    target_type VARCHAR(255) NOT NULL DEFAULT 'TICKET',
    min_ticket_qty INTEGER NOT NULL DEFAULT 0,
    min_combo_qty INTEGER NOT NULL DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ── REFRESH_TOKENS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP
);

-- ── STAFF_PROFILES ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS staff_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    cinema_id UUID NOT NULL REFERENCES cinemas(id),
    employee_code VARCHAR(50),
    created_at TIMESTAMP
);

-- ── SYSTEM_CONFIGS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS system_configs (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ── USER_EXP_HISTORY ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_exp_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    amount BIGINT NOT NULL,
    reason VARCHAR(100),
    reference_id VARCHAR(100),
    created_at TIMESTAMP
);
